package com.example.expensetracker.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.expensetracker.data.entity.Transaction;

/**
 * TFLite-based anomaly detector for daily spending.
 * Model: anomaly_detector.tflite (assets/)
 *   Input  float32[1][3]
 *              [0] daily_amount / NORM_MAX   (clamped 0-1)
 *              [1] day_of_week  / 6          (0=Mon, 6=Sun)
 *              [2] day_of_month / 30         (1-based day / 30)
 *   Output float32[1][1] — anomaly probability; >= ANOMALY_THRESHOLD -> flagged
 */
public class AnomalyDetector {

    private static final String TAG        = "AnomalyDetector";
    private static final String MODEL_FILE = "com/example/expensetracker/ml/anomaly_detector.tflite";

    public static final float NORM_MAX          = 50_000f;
    public static final float ANOMALY_THRESHOLD = 0.50f;

    /** One anomalous day detected by the model. */
    public static class AnomalyResult {
        public final long   dateMs;
        public final double totalAmount;
        public final float  anomalyScore;

        public AnomalyResult(long dateMs, double totalAmount, float anomalyScore) {
            this.dateMs       = dateMs;
            this.totalAmount  = totalAmount;
            this.anomalyScore = anomalyScore;
        }
    }

    private Interpreter interpreter;
    private boolean     isInitialized = false;

    public AnomalyDetector(Context context) {
        try {
            MappedByteBuffer buf = loadModelFile(context, MODEL_FILE);
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(2);
            interpreter   = new Interpreter(buf, opts);
            isInitialized = true;
            Log.d(TAG, "AnomalyDetector initialized successfully.");
        } catch (Exception e) {
            // Catches IOException (file missing) AND IllegalArgumentException
            // (op version mismatch) so the app never crashes here.
            Log.e(TAG, "Cannot load " + MODEL_FILE + ": " + e.getMessage());
            isInitialized = false;
        }
    }

    public List<AnomalyResult> detectAnomalies(List<Transaction> transactions) {
        List<AnomalyResult> results = new ArrayList<>();
        if (!isInitialized || transactions == null || transactions.isEmpty()) {
            return results;
        }

        // Group DEBIT amounts by calendar day (midnight timestamp)
        Map<Long, Double> dailyTotals = new TreeMap<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction txn : transactions) {
            if (!"DEBIT".equals(txn.transactionType)) continue;
            cal.setTimeInMillis(txn.timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE,      0);
            cal.set(Calendar.SECOND,      0);
            cal.set(Calendar.MILLISECOND, 0);
            long dayKey = cal.getTimeInMillis();
            dailyTotals.merge(dayKey, txn.amount, Double::sum);
        }

        // Score each day with the model
        float[][] input  = new float[1][3];
        float[][] output = new float[1][1];

        for (Map.Entry<Long, Double> entry : dailyTotals.entrySet()) {
            long   dayMs  = entry.getKey();
            double amount = entry.getValue();

            cal.setTimeInMillis(dayMs);

            int calDow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon ... 7=Sat
            int dow    = (calDow == Calendar.SUNDAY) ? 6 : calDow - 2;
            int dom    = cal.get(Calendar.DAY_OF_MONTH);

            input[0][0] = (float) Math.min(amount / NORM_MAX, 1.0);
            input[0][1] = dow / 6.0f;
            input[0][2] = dom / 30.0f;

            try {
                interpreter.run(input, output);
                float score = output[0][0];
                if (score >= ANOMALY_THRESHOLD) {
                    results.add(new AnomalyResult(dayMs, amount, score));
                }
            } catch (Exception e) {
                Log.e(TAG, "Inference error for day " + dayMs + ": " + e.getMessage());
            }
        }

        results.sort((a, b) -> Float.compare(b.anomalyScore, a.anomalyScore));
        return results;
    }

    public boolean isReady() {
        return isInitialized;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        isInitialized = false;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(modelName);
        FileInputStream     fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel         fc  = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }
}
