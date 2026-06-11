package com.example.expensetracker.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry of bank → SMS Sender ID mappings with debit-SMS reliability data.
 *
 * ── Debit SMS policy (April 2026) ────────────────────────────────────────────
 *
 *  RELIABLE  (no threshold, SMS sent for every debit amount):
 *    SBI, Canara Bank, Indian Bank, IOB, KVB, SIB, Federal Bank, TMB, CUB,
 *    Dhanlaxmi, CSB, Karnataka Bank, BOB, BOI, PNB, Union Bank, Central Bank,
 *    IDFC First, IndusInd, RBL, AU SFB, Bandhan
 *
 *  RESTRICTED  (SMS skipped for debit < ₹100 — use QR Scan as backup):
 *    HDFC Bank, ICICI Bank, Axis Bank, Kotak Mahindra Bank
 *
 * ── TRAI prefix format ────────────────────────────────────────────────────────
 *  Arrives as XX-YYYYYY.  Only YYYYYY is stored here.
 *  SenderIdExtractor.strip() removes the prefix before matching.
 */
public final class BankSenderRegistry {

    private BankSenderRegistry() {}

    // Shared warning string for all RESTRICTED banks
    private static final String RESTRICTED_WARNING =
            "⚠️ This bank does NOT send SMS for debit transactions below ₹100. " +
            "Small UPI payments will be missed by SMS tracking. " +
            "Use the QR Scan feature as a backup to log those transactions manually.";

    private static final Map<String, BankSenderInfo> REGISTRY = new LinkedHashMap<>();

    static {

        // ════════════════════════════════════════════════════════════════════
        // RELIABLE BANKS — South Indian (listed first for Coimbatore users)
        // ════════════════════════════════════════════════════════════════════

        add("State Bank of India (SBI)",
            "SBIINB",
            opts(
                opt("SBIINB", "UPI / NEFT / Net banking — most common"),
                opt("SBIBNK", "General banking alerts"),
                opt("SBIPSG", "Payment gateway"),
                opt("SBYONO", "YONO app alerts"),
                opt("ATMSBI", "ATM & debit card"),
                opt("ATMSMS", "ATM alternate"),
                opt("SBICRD", "SBI Credit Card")
            ),
            "SBI sends SMS for all debit amounts — no threshold. " +
            "SBIINB covers most UPI/NEFT. ATM transactions arrive from ATMSBI.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Canara Bank",
            "CANARB",
            opts(
                opt("CANARB", "Most common — savings & UPI"),
                opt("CANBNK", "Alternate general alerts"),
                opt("CANMNY", "Canara Money / digital wallet"),
                opt("CANRRB", "Rural / Gramin branch"),
                opt("CAANBK", "Alternate variant")
            ),
            "Canara Bank reliably sends SMS for all debit amounts. " +
            "CANARB covers debit & UPI for most South India users.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Indian Bank",
            "INDBNK",
            opts(
                opt("INDBNK", "Most common — savings & UPI"),
                opt("INBUPI", "UPI-specific alerts")
            ),
            "Indian Bank reliably sends SMS for all debit amounts. " +
            "INDBNK covers savings accounts; INBUPI is UPI-specific.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Indian Overseas Bank (IOB)",
            "IOBBNK",
            opts(
                opt("IOBBNK", "Most common"),
                opt("IOBANK", "Alternate"),
                opt("IOBBQR", "QR / UPI"),
                opt("IOBATM", "ATM transactions"),
                opt("IOBOTP", "OTP / 2FA")
            ),
            "IOB reliably sends SMS for all debit amounts. " +
            "IOBBNK covers most debit and UPI alerts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Karur Vysya Bank (KVB)",
            "KVBANK",
            opts(
                opt("KVBANK", "Most common"),
                opt("KVBUPI", "UPI-specific"),
                opt("KVBOTP", "OTP alerts"),
                opt("KVBBNK", "Alternate variant")
            ),
            "KVB reliably sends SMS for all debit amounts. " +
            "KVBANK covers most alerts; KVBUPI is UPI-specific.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("South Indian Bank (SIB)",
            "SIBBNK",
            opts(
                opt("SIBBNK", "Most common — savings & UPI"),
                opt("SOUTHIN", "Alternate")
            ),
            "SIB reliably sends SMS for all debit amounts. " +
            "SIBBNK covers debit, UPI, and NEFT.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Federal Bank",
            "FEDBNK",
            opts(
                opt("FEDBNK", "Most common — savings & UPI"),
                opt("FEDOTP", "OTP / 2FA"),
                opt("FEDADV", "Advisory alerts")
            ),
            "Federal Bank reliably sends SMS for all debit amounts. " +
            "FEDBNK covers all debit and UPI transaction SMS.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Tamilnad Mercantile Bank (TMB)",
            "TMBBNK",
            opts(
                opt("TMBBNK", "Primary — all transaction types")
            ),
            "TMB reliably sends SMS for all debit amounts via TMBBNK.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("City Union Bank (CUB)",
            "CUBLTD",
            opts(
                opt("CUBLTD", "Most common"),
                opt("CUBANK", "Alternate"),
                opt("CUBUPI", "UPI-specific"),
                opt("CUBSMS", "General alerts"),
                opt("CUBFST", "FASTag / digital"),
                opt("CUBOTP", "OTP")
            ),
            "CUB reliably sends SMS for all debit amounts. " +
            "CUBLTD and CUBUPI are most common for transactions.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Dhanlaxmi Bank",
            "DHANBK",
            opts(opt("DHANBK", "Primary")),
            "Dhanlaxmi Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("CSB Bank (Catholic Syrian Bank)",
            "CSBBNK",
            opts(opt("CSBBNK", "Primary")),
            "CSB Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Karnataka Bank",
            "KARBNK",
            opts(
                opt("KARBNK", "Most common"),
                opt("KBLBNK", "Alternate"),
                opt("KTKBNK", "Alternate variant"),
                opt("KRNBNK", "Alternate variant")
            ),
            "Karnataka Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        // ════════════════════════════════════════════════════════════════════
        // RELIABLE BANKS — Pan-India
        // ════════════════════════════════════════════════════════════════════

        add("Bank of Baroda",
            "BOBBNK",
            opts(
                opt("BOBBNK", "Most common"),
                opt("BOBUPI", "UPI-specific"),
                opt("BOBSMS", "General alerts"),
                opt("BOBTXN", "Transaction alerts")
            ),
            "Bank of Baroda reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Bank of India",
            "BOIIND",
            opts(
                opt("BOIIND", "Most common"),
                opt("BOIBAL", "Balance alerts"),
                opt("BOISAF", "Safety / OTP")
            ),
            "Bank of India reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Punjab National Bank (PNB)",
            "PNBSMS",
            opts(
                opt("PNBSMS", "Most common"),
                opt("PNBOTP", "OTP"),
                opt("PNBCRD", "Credit card")
            ),
            "PNB reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Union Bank of India",
            "UBIBNK",
            opts(opt("UBIBNK", "Most common")),
            "Union Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Central Bank of India",
            "CENTBK",
            opts(
                opt("CENTBK", "Most common"),
                opt("CBIOTP", "OTP")
            ),
            "Central Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("IDFC First Bank",
            "IDFCBK",
            opts(
                opt("IDFCBK", "Most common"),
                opt("IDFCFB", "First Bank variant"),
                opt("IDFCIT", "IT / digital alerts")
            ),
            "IDFC First Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("IndusInd Bank",
            "INDUSB",
            opts(
                opt("INDUSB", "Most common"),
                opt("INDUSO", "Online alerts"),
                opt("INDUSA", "Alternate")
            ),
            "IndusInd Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("RBL Bank",
            "RBLBNK",
            opts(
                opt("RBLBNK", "Most common"),
                opt("RBLCCC", "Credit card")
            ),
            "RBL Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("AU Small Finance Bank",
            "AUBANK",
            opts(
                opt("AUBANK", "Most common"),
                opt("AUBSMS", "SMS alerts"),
                opt("AUBMSG", "Message gateway")
            ),
            "AU Small Finance Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        add("Bandhan Bank",
            "BNDNBK",
            opts(
                opt("BNDNBK", "Most common"),
                opt("BDNSMS", "SMS alerts")
            ),
            "Bandhan Bank reliably sends SMS for all debit amounts.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );

        // ════════════════════════════════════════════════════════════════════
        // RESTRICTED BANKS — SMS skipped for debit < ₹100
        // ════════════════════════════════════════════════════════════════════

        add("HDFC Bank",
            "HDFCBK",
            opts(
                opt("HDFCBK", "Savings & UPI — most common"),
                opt("HDFCBN", "Alternate savings"),
                opt("HDFCAL", "Alerts"),
                opt("HDFCBA", "Banking alerts"),
                opt("HDFCSD", "Savings debit")
            ),
            "HDFC sends SMS for debits ≥ ₹100 only. " +
            "Use QR Scan to log small UPI payments.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        add("HDFC Credit Card",
            "HDFCCC",
            opts(
                opt("HDFCCC", "Credit card transactions"),
                opt("HDFCRD", "Credit card alternate"),
                opt("HDFCGC", "Gold card")
            ),
            "HDFC credit card SMS always uses HDFCCC. " +
            "Still subject to the < ₹100 threshold.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        add("ICICI Bank",
            "ICICIB",
            opts(
                opt("ICICIB", "Savings & UPI — most common"),
                opt("ICICIH", "Home / mortgage"),
                opt("ICICIK", "KYC alerts"),
                opt("ICIOTP", "OTP / 2FA")
            ),
            "ICICI is highly restricted — SMS skipped for debits < ₹100. " +
            "Use QR Scan to log small UPI payments.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        add("ICICI Credit Card",
            "ICICIC",
            opts(opt("ICICIC", "Credit card — primary")),
            "ICICI credit card SMS always uses ICICIC. " +
            "Still subject to the < ₹100 threshold.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        add("Axis Bank",
            "AXISBK",
            opts(
                opt("AXISBK", "Savings & UPI — most common"),
                opt("AXISB",  "Short variant"),
                opt("AXISHR", "HR / payroll"),
                opt("AXISMR", "Merchant receipts"),
                opt("AXSFIN", "Axis Finance")
            ),
            "Axis Bank is highly restricted — SMS skipped for debits < ₹100. " +
            "Use QR Scan to log small UPI payments.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        add("Kotak Mahindra Bank",
            "KOTAKB",
            opts(
                opt("KOTAKB", "Savings & UPI — most common"),
                opt("KOTAKP", "Promotional / offers"),
                opt("KTKREM", "Remittance")
            ),
            "Kotak does not send SMS for debits < ₹100. " +
            "Use QR Scan to log small UPI payments.",
            BankSenderInfo.SmsReliability.RESTRICTED,
            RESTRICTED_WARNING
        );

        // ════════════════════════════════════════════════════════════════════
        // OTHER / CUSTOM
        // ════════════════════════════════════════════════════════════════════

        add("Other / Custom",
            "",
            opts(opt("", "Type your bank's SMS Sender ID manually")),
            "Open your bank's transaction SMS and note the 6 characters after the hyphen. " +
            "Example: in 'AD-SBIINB', the Sender ID is 'SBIINB'.",
            BankSenderInfo.SmsReliability.RELIABLE,
            null
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static List<String> getBankNames() {
        return Collections.unmodifiableList(
                Arrays.asList(REGISTRY.keySet().toArray(new String[0])));
    }

    public static BankSenderInfo getInfo(String bankName) {
        return REGISTRY.get(bankName);
    }

    public static String getPrimary(String bankName) {
        BankSenderInfo info = REGISTRY.get(bankName);
        return info != null ? info.primarySenderId : "";
    }

    /** Returns true if the given bank may skip SMS for small debits. */
    public static boolean isRestricted(String bankName) {
        BankSenderInfo info = REGISTRY.get(bankName);
        return info != null && info.isSmsRestricted();
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private static void add(String name, String primary,
                            List<BankSenderInfo.SenderOption> options,
                            String note,
                            BankSenderInfo.SmsReliability reliability,
                            String debitWarning) {
        REGISTRY.put(name, new BankSenderInfo(
                name, primary, options, note, reliability, debitWarning));
    }

    private static List<BankSenderInfo.SenderOption> opts(BankSenderInfo.SenderOption... items) {
        return Arrays.asList(items);
    }

    private static BankSenderInfo.SenderOption opt(String id, String label) {
        return new BankSenderInfo.SenderOption(id, label);
    }
}
