package com.example.expensetracker.utils;

/**
 * Utility to strip the TRAI-mandated operator+circle prefix from an SMS sender address.
 *
 * TRAI format (since 2008):  XX-YYYYYY
 *   XX      = 2-letter prefix (operator code + region code), e.g. "AD", "VK", "JM"
 *   -       = literal hyphen separator
 *   YYYYYY  = 6-character registered header, e.g. "SBIINB", "HDFCBK"
 *
 * Examples:
 *   "AD-SBIINB"  → "SBIINB"
 *   "VK-HDFCBK"  → "HDFCBK"
 *   "JM-AXISBK"  → "AXISBK"
 *   "HDFCBK"     → "HDFCBK"   (already stripped / no prefix)
 *
 * Why this matters for SmsReceiver:
 *   Android delivers the originating address exactly as the network sends it,
 *   which always includes the prefix on Indian transactional SMS.
 *   Your BankAccount.smsSenderId stores only the 6-char header (e.g. "SBIINB"),
 *   so we must strip before comparing.
 */
public final class SenderIdExtractor {

    private SenderIdExtractor() {}

    /**
     * Returns the 6-char header portion of a sender ID.
     * If the input doesn't match XX-YYYYYY, returns the original string uppercased.
     */
    public static String strip(String rawSender) {
        if (rawSender == null) return "";
        String s = rawSender.trim().toUpperCase();

        // Pattern: exactly 2 alpha chars, a hyphen, then 4-8 alphanumeric chars
        if (s.length() >= 5 && s.charAt(2) == '-') {
            String prefix = s.substring(0, 2);
            String header = s.substring(3);
            // Validate prefix is two letters, header is alphanumeric
            if (prefix.matches("[A-Z]{2}") && header.matches("[A-Z0-9]{4,8}")) {
                return header;
            }
        }
        return s; // already stripped or non-standard
    }

    /**
     * Checks whether the stripped header contains the given bank keyword.
     * Useful as a fallback when no exact pattern row exists.
     *
     * E.g. containsKeyword("SBIINB", "SBI") → true
     *      containsKeyword("HDFCBK", "HDFC") → true
     */
    public static boolean containsKeyword(String strippedHeader, String keyword) {
        if (strippedHeader == null || keyword == null) return false;
        return strippedHeader.toUpperCase().contains(keyword.toUpperCase());
    }
}
