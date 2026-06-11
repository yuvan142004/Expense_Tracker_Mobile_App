package com.example.expensetracker.data;

import java.util.List;

/**
 * Holds all SMS Sender ID options + debit SMS reliability info for one bank.
 */
public class BankSenderInfo {

    // ── SMS reliability tiers ─────────────────────────────────────────────────

    public enum SmsReliability {
        /**
         * RELIABLE — SMS sent for all debit amounts, no known threshold.
         * Examples: SBI, Canara, Indian Bank, KVB, SIB, Federal.
         */
        RELIABLE,

        /**
         * RESTRICTED — SMS skipped for small debits (typically < ₹100).
         * Users of these banks should use QR Scan as backup.
         * Examples: HDFC, ICICI, Axis, Kotak.
         */
        RESTRICTED
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public final String         bankName;
    public final String         primarySenderId;
    public final List<SenderOption> options;
    public final String         helperNote;       // shown below the sender ID field
    public final SmsReliability smsReliability;
    public final String         debitWarning;     // shown as banner for RESTRICTED banks (null if RELIABLE)

    public BankSenderInfo(String bankName,
                          String primarySenderId,
                          List<SenderOption> options,
                          String helperNote,
                          SmsReliability smsReliability,
                          String debitWarning) {
        this.bankName        = bankName;
        this.primarySenderId = primarySenderId;
        this.options         = options;
        this.helperNote      = helperNote;
        this.smsReliability  = smsReliability;
        this.debitWarning    = debitWarning;
    }

    /** Returns true if this bank may skip SMS for small debit transactions. */
    public boolean isSmsRestricted() {
        return smsReliability == SmsReliability.RESTRICTED;
    }

    // ── Inner option ──────────────────────────────────────────────────────────

    public static class SenderOption {
        public final String senderId;
        public final String label;

        public SenderOption(String senderId, String label) {
            this.senderId = senderId;
            this.label    = label;
        }

        @Override public String toString() { return label + " (" + senderId + ")"; }
    }
}
