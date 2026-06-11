package com.example.expensetracker.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import com.example.expensetracker.data.dao.TransactionDao;

/**
 * TFLite-based next-month spending predictor.
 * Model: spending_predictor.tflite (assets/)
 *   Input  float32[1][30]  — 3 months x 10 categories, each value / NORM_MAX
 *   Output float32[1][10]  — predicted totals per category x NORM_MAX = INR
 */
public class SpendingPredictor {

    private static final String TAG        = "SpendingPredictor";
    private static final String MODEL_FILE = "com/example/expensetracker/ml/spending_predictor.tflite";

    public static final float NORM_MAX       = 50_000f;
    public static final int   NUM_CATEGORIES = 10;
    public static final int   NUM_MONTHS_IN  = 3;
    private static final int  INPUT_SIZE     = NUM_MONTHS_IN * NUM_CATEGORIES; // 30

    private Interpreter interpreter;
    private boolean     isInitialized = false;

    public SpendingPredictor(Context context) {
        try {
            MappedByteBuffer buf = loadModelFile(context, MODEL_FILE);
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(2);
            interpreter   = new Interpreter(buf, opts);
            isInitialized = true;
            Log.d(TAG, "SpendingPredictor initialized successfully.");
        } catch (Exception e) {
            // Catches IOException (file missing) AND IllegalArgumentException
            // (op version mismatch) so the app never crashes here.
            Log.e(TAG, "Cannot load " + MODEL_FILE + ": " + e.getMessage());
            isInitialized = false;
        }
    }

    public float[] predictNextMonth(List<List<TransactionDao.CategorySum>> last3Months) {
        if (!isInitialized) return null;
        if (last3Months == null || last3Months.size() < NUM_MONTHS_IN) return null;

        float[][] input = new float[1][INPUT_SIZE];
        for (int m = 0; m < NUM_MONTHS_IN; m++) {
            float[] monthTotals = extractCategoryTotals(last3Months.get(m));
            for (int c = 0; c < NUM_CATEGORIES; c++) {
                input[0][m * NUM_CATEGORIES + c] =
                    Math.min(monthTotals[c], NORM_MAX) / NORM_MAX;
            }
        }

        float[][] output = new float[1][NUM_CATEGORIES];
        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference failed: " + e.getMessage());
            return null;
        }

        float[] predictions = new float[NUM_CATEGORIES];
        for (int i = 0; i < NUM_CATEGORIES; i++) {
            predictions[i] = Math.max(0f, output[0][i] * NORM_MAX);
        }
        return predictions;
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

    private float[] extractCategoryTotals(List<TransactionDao.CategorySum> sums) {
        float[] totals = new float[NUM_CATEGORIES];
        if (sums == null) return totals;
        for (TransactionDao.CategorySum cs : sums) {
            int idx = cs.categoryId - 1;
            if (idx >= 0 && idx < NUM_CATEGORIES) {
                totals[idx] = (float) cs.total;
            }
        }
        return totals;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(modelName);
        FileInputStream     fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel         fc  = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }
}
