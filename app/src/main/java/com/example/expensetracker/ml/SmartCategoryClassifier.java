package com.example.expensetracker.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;

/**
 * TFLite-based smart category classifier.
 * Falls back to this when ExpenseClassifier keyword matching returns CAT_OTHER (10).
 * Model: category_classifier.tflite (assets/)
 *   Input  float32[1][3]
 *              [0] amount      / AMOUNT_MAX  (clamped 0-1)
 *              [1] hour_of_day / 23          (0=midnight, 1=11pm)
 *              [2] day_of_week / 6           (0=Monday, 6=Sunday)
 *   Output float32[1][10] — softmax probs; argmax+1 = categoryId (1-10)
 */
public class SmartCategoryClassifier {

    private static final String TAG        = "SmartCategoryClassifier";
    private static final String MODEL_FILE = "com/example/expensetracker/ml/category_classifier.tflite";

    public static final float AMOUNT_MAX    = 50_000f;
    public static final int   NUM_CATEGORIES = 10;

    private static final String[] CATEGORY_NAMES = {
        "Food", "Shopping", "Fund Transfer", "Friend",
        "Bills & Utilities", "Transport", "Health",
        "Entertainment", "Education", "Other"
    };

    private Interpreter interpreter;
    private boolean     isInitialized = false;

    public SmartCategoryClassifier(Context context) {
        try {
            MappedByteBuffer buf = loadModelFile(context, MODEL_FILE);
            Interpreter.Options opts = new Interpreter.Options();
            opts.setNumThreads(2);
            interpreter   = new Interpreter(buf, opts);
            isInitialized = true;
            Log.d(TAG, "SmartCategoryClassifier initialized successfully.");
        } catch (Exception e) {
            // Catches IOException (file missing) AND IllegalArgumentException
            // (op version mismatch) so the app never crashes here.
            Log.e(TAG, "Cannot load " + MODEL_FILE + ": " + e.getMessage());
            isInitialized = false;
        }
    }

    /**
     * Classifies a transaction into a spending category (1-10).
     * Returns 10 (Other) if model unavailable.
     */
    public int classify(double amount, long timestampMs) {
        float[] result = runInference(amount, timestampMs);
        if (result == null) return 10;
        int best = 0;
        for (int i = 1; i < NUM_CATEGORIES; i++) {
            if (result[i] > result[best]) best = i;
        }
        return best + 1;
    }

    /**
     * Classifies with confidence. Returns float[2]: [categoryId, confidence].
     * Returns [10, 0.0] if model unavailable.
     */
    public float[] classifyWithConfidence(double amount, long timestampMs) {
        float[] probs = runInference(amount, timestampMs);
        if (probs == null) return new float[]{10f, 0f};
        int   best  = 0;
        float score = probs[0];
        for (int i = 1; i < NUM_CATEGORIES; i++) {
            if (probs[i] > score) { score = probs[i]; best = i; }
        }
        return new float[]{best + 1, score};
    }

    public static String getCategoryName(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= NUM_CATEGORIES) return "Other";
        return CATEGORY_NAMES[zeroBasedIndex];
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

    private float[] runInference(double amount, long timestampMs) {
        if (!isInitialized) return null;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMs);

        int hour   = cal.get(Calendar.HOUR_OF_DAY);
        int calDow = cal.get(Calendar.DAY_OF_WEEK);
        int dow    = (calDow == Calendar.SUNDAY) ? 6 : calDow - 2;

        float[][] input  = new float[1][3];
        input[0][0] = (float) Math.min(amount / AMOUNT_MAX, 1.0);
        input[0][1] = hour / 23.0f;
        input[0][2] = dow  /  6.0f;

        float[][] output = new float[1][NUM_CATEGORIES];
        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference failed: " + e.getMessage());
            return null;
        }
        return output[0];
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor afd = context.getAssets().openFd(modelName);
        FileInputStream     fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel         fc  = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }
}
