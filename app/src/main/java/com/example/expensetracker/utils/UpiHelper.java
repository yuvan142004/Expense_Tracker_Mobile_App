package com.example.expensetracker.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UpiHelper {

    private UpiHelper() {}

    public static final int UPI_REQUEST_CODE = 1001;

    /**
     * Build a UPI payment URI.
     *
     * @param pa    Payee UPI ID (e.g. merchant@upi)
     * @param pn    Payee name
     * @param am    Amount as string (e.g. "250.00")
     * @param tn    Transaction note
     * @param cu    Currency (typically "INR")
     */
    public static Intent buildUpiIntent(String pa, String pn, String am,
                                        String tn, String cu) {
        Uri uri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", pa)
                .appendQueryParameter("pn", pn != null ? pn : "")
                .appendQueryParameter("am", am)
                .appendQueryParameter("tn", tn != null ? tn : "Expense Tracker Payment")
                .appendQueryParameter("cu", cu != null ? cu : "INR")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Parse a raw UPI QR string into its components.
     * Returns a UpiPaymentData object, or null if the string is not a valid UPI URI.
     *
     * Typical QR format: upi://pay?pa=xxx&pn=xxx&am=xxx&tn=xxx&cu=INR
     */
    public static UpiPaymentData parseQrString(String qrContent) {
        if (qrContent == null || !qrContent.startsWith("upi://")) return null;
        try {
            Uri uri = Uri.parse(qrContent);
            UpiPaymentData data = new UpiPaymentData();
            data.pa = uri.getQueryParameter("pa");
            data.pn = uri.getQueryParameter("pn");
            data.am = uri.getQueryParameter("am");
            data.tn = uri.getQueryParameter("tn");
            data.cu = uri.getQueryParameter("cu");
            if (data.pa == null || data.pa.isEmpty()) return null; // payee address is mandatory
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check whether a UPI intent result indicates success.
     * Apps return "SUCCESS" / "FAILURE" / "SUBMITTED" in the response.
     */
    public static boolean isUpiSuccess(Intent data) {
        if (data == null) return false;
        String response = data.getStringExtra("response");
        if (response == null) return false;
        return response.toLowerCase().contains("success");
    }

    /**
     * Extract UPI transaction reference from the UPI callback intent.
     */
    public static String extractUpiRef(Intent data) {
        if (data == null) return null;
        String response = data.getStringExtra("response");
        if (response == null) return null;
        // response format: "txnId=xxx&responseCode=00&..."
        String[] parts = response.split("&");
        for (String part : parts) {
            if (part.toLowerCase().startsWith("txnid=") ||
                part.toLowerCase().startsWith("txnref=")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) return kv[1];
            }
        }
        return null;
    }

    public static class UpiPaymentData {
        public String pa;  // Payee UPI ID
        public String pn;  // Payee name
        public String am;  // Amount
        public String tn;  // Transaction note
        public String cu;  // Currency
    }
}
