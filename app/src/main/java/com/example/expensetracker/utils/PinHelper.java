package com.example.expensetracker.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PinHelper {

    private PinHelper() {}

    /** Hash a PIN string using SHA-256. Returns hex string. */
    public static String hash(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Verify a plain PIN against a stored hash. */
    public static boolean verify(String pin, String storedHash) {
        if (pin == null || storedHash == null) return false;
        return hash(pin).equals(storedHash);
    }

    /** Validate PIN format: 4–6 digits only. */
    public static boolean isValidFormat(String pin) {
        return pin != null && pin.matches("\\d{4,6}");
    }
}
