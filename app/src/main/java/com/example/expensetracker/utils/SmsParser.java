package com.example.expensetracker.utils;

import com.example.expensetracker.data.entity.SmsPattern;
import com.example.expensetracker.data.entity.Transaction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    private SmsParser() {}

    public static class ParsedSms {
        public double amount      = 0;
        public String merchant    = null;
        public String type        = "DEBIT"; // "DEBIT" or "CREDIT"
        public String upiRef      = null;
        public long   timestamp   = System.currentTimeMillis();
        public String rawSms      = null;
    }

    /**
     * Parse an SMS body using the given SmsPattern.
     * Returns null if the amount cannot be extracted (discard silently).
     */
    public static ParsedSms parse(String smsBody, SmsPattern pattern) {
        if (smsBody == null || pattern == null) return null;

        ParsedSms result = new ParsedSms();
        result.rawSms = smsBody;

        // ── Amount ──────────────────────────────────────────────────────────
        try {
            Matcher m = Pattern.compile(pattern.amountRegex,
                Pattern.CASE_INSENSITIVE).matcher(smsBody);
            if (m.find()) {
                String raw = m.group(1).replace(",", "").trim();
                result.amount = Double.parseDouble(raw);
            } else {
                return null; // Cannot determine amount — discard
            }
        } catch (Exception e) {
            return null;
        }

        // ── Merchant ────────────────────────────────────────────────────────
        try {
            if (pattern.merchantRegex != null && !pattern.merchantRegex.isEmpty()) {
                Matcher m = Pattern.compile(pattern.merchantRegex,
                    Pattern.CASE_INSENSITIVE).matcher(smsBody);
                if (m.find()) {
                    result.merchant = m.group(1).trim();
                }
            }
        } catch (Exception ignored) {}

        // ── Transaction Type ─────────────────────────────────────────────────
        try {
            if (pattern.typeRegex != null && !pattern.typeRegex.isEmpty()) {
                Matcher m = Pattern.compile(pattern.typeRegex,
                    Pattern.CASE_INSENSITIVE).matcher(smsBody);
                if (m.find()) {
                    String matched = m.group(0).toLowerCase();
                    boolean isCredit = matched.contains("credit") ||
                                       matched.contains("credited") ||
                                       matched.contains("received") ||
                                       matched.contains("refund");
                    result.type = isCredit ? "CREDIT" : "DEBIT";
                }
            }
        } catch (Exception ignored) {}

        // ── UPI Reference ────────────────────────────────────────────────────
        try {
            if (pattern.refRegex != null && !pattern.refRegex.isEmpty()) {
                Matcher m = Pattern.compile(pattern.refRegex,
                    Pattern.CASE_INSENSITIVE).matcher(smsBody);
                if (m.find()) {
                    result.upiRef = m.group(1).trim();
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Quick check: does this SMS look like a transaction SMS at all?
     * Used as a fast pre-filter before full regex parsing.
     */
    public static boolean looksLikeTransaction(String smsBody) {
        if (smsBody == null) return false;
        String lower = smsBody.toLowerCase();
        return (lower.contains("debited") || lower.contains("credited") ||
                lower.contains("rs.") || lower.contains("inr") ||
                lower.contains("upi") || lower.contains("transfer") ||
                lower.contains("paid") || lower.contains("withdrawn"));
    }
}
