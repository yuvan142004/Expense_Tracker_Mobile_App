package com.example.expensetracker.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expensetracker.data.dao.*;
import com.example.expensetracker.data.entity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        User.class,
        BankAccount.class,
        Category.class,
        Transaction.class,
        BudgetLimit.class,
        RecurringExpense.class,
        SmsPattern.class
    },
    version = 4,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract BankAccountDao bankAccountDao();
    public abstract CategoryDao categoryDao();
    public abstract TransactionDao transactionDao();
    public abstract BudgetLimitDao budgetLimitDao();
    public abstract RecurringExpenseDao recurringExpenseDao();
    public abstract SmsPatternDao smsPatternDao();

    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expense_tracker.db"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(PRE_POPULATE_CALLBACK)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // ── Migration 1 → 2 ─────────────────────────────────────────────────────

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN profilePicturePath TEXT");
        }
    };

    // ── Migration 2 → 3 ─────────────────────────────────────────────────────

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add new fields to transactions table
            db.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT DEFAULT 'COMPLETED'");
            db.execSQL("ALTER TABLE transactions ADD COLUMN upiAppUsed TEXT");
            db.execSQL("ALTER TABLE transactions ADD COLUMN scannedQrData TEXT");

            // Add new fields to bank_accounts table
            db.execSQL("ALTER TABLE bank_accounts ADD COLUMN enableSmsTracking INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE bank_accounts ADD COLUMN enableUpiTracking INTEGER DEFAULT 0");

            // Update enableUpiTracking to TRUE for restricted banks
            db.execSQL("UPDATE bank_accounts SET enableUpiTracking = 1 WHERE " +
                      "bankName IN ('HDFC Bank', 'HDFC Credit Card', 'ICICI Bank', " +
                      "'ICICI Credit Card', 'Axis Bank', 'Kotak Mahindra Bank')");
        }
    };

    // ── Migration 3 → 4 ─────────────────────────────────────────────────────

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add paymentMethod field to transactions table
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT DEFAULT 'CASH'");
            
            // Update existing UPI transactions to have UPI as payment method
            db.execSQL("UPDATE transactions SET paymentMethod = 'UPI' WHERE " +
                      "source = 'QR_UPI' OR source = 'QR_UPI_MANUAL' OR upiAppUsed IS NOT NULL");
            
            // Update existing SMS transactions based on transaction type
            // If UPI ref exists, it's likely a UPI transaction
            db.execSQL("UPDATE transactions SET paymentMethod = 'UPI' WHERE " +
                      "source = 'SMS' AND upiRef IS NOT NULL");
        }
    };

    // ── Pre-populate callback ─────────────────────────────────────────────────

    private static final Callback PRE_POPULATE_CALLBACK = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            dbExecutor.execute(() -> {
                AppDatabase database = INSTANCE;
                if (database != null) {
                    populateCategories(database.categoryDao());
                    populateSmsPatterns(database.smsPatternDao());
                }
            });
        }
    };

    // ── Categories ────────────────────────────────────────────────────────────

    private static void populateCategories(CategoryDao dao) {
        List<Category> defaults = new ArrayList<>();
        defaults.add(makeCategory("Food",              "#FF6B6B", "ic_food",          true));
        defaults.add(makeCategory("Shopping",          "#4ECDC4", "ic_shopping",      true));
        defaults.add(makeCategory("Fund Transfer",     "#45B7D1", "ic_transfer",      true));
        defaults.add(makeCategory("Friend",            "#96CEB4", "ic_friend",        true));
        defaults.add(makeCategory("Bills & Utilities", "#FFEAA7", "ic_bills",         true));
        defaults.add(makeCategory("Transport",         "#DDA0DD", "ic_transport",     true));
        defaults.add(makeCategory("Health & Medical",  "#98D8C8", "ic_health",        true));
        defaults.add(makeCategory("Entertainment",     "#F7DC6F", "ic_entertainment", true));
        defaults.add(makeCategory("Education",         "#85C1E9", "ic_education",     true));
        defaults.add(makeCategory("Other",             "#BDC3C7", "ic_other",         true));
        dao.insertAll(defaults);
    }

    private static Category makeCategory(String name, String color,
                                          String icon, boolean isDefault) {
        Category c = new Category();
        c.name      = name;
        c.colorHex  = color;
        c.iconRes   = icon;
        c.isDefault = isDefault;
        return c;
    }

    // ── SMS Patterns ──────────────────────────────────────────────────────────
    //
    // Each bank's pattern captures:
    //   amountRegex   — extracts the rupee amount
    //   merchantRegex — extracts the payee/merchant name
    //   typeRegex     — determines DEBIT vs CREDIT
    //   refRegex      — extracts UPI reference / transaction ID
    //
    // The sender IDs here must match 6-char DLT headers (no XX- prefix).

    private static void populateSmsPatterns(SmsPatternDao dao) {
        List<SmsPattern> patterns = new ArrayList<>();

        // ── SBI ──────────────────────────────────────────────────────────────
        // Core savings/UPI header
        patterns.add(make("SBI",         "SBIINB",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|in favour of|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|withdrawn|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        // General banking alerts (SBIBNK)
        patterns.add(make("SBI",         "SBIBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|credited|credit|paid)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));
        // Payment gateway (SBIPSG)
        patterns.add(make("SBI",         "SBIPSG",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|for)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|purchase)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));
        // SBI Credit Card
        patterns.add(make("SBI Credit",  "SBICRD",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|spent|purchase|used|credited|refund)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{6,20})"
        ));
        // ATM
        patterns.add(make("SBI ATM",     "ATMSBI",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|from)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(withdrawn|debited|debit|ATM)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));
        patterns.add(make("SBI ATM",     "ATMSMS",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|from)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(withdrawn|debited|ATM)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));

        // ── Canara Bank ───────────────────────────────────────────────────────
        patterns.add(make("Canara Bank", "CANARB",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|paid to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("Canara Bank", "CANBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|credited|credit|paid)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));
        patterns.add(make("Canara Bank", "CANMNY",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|for)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));

        // ── Indian Bank ───────────────────────────────────────────────────────
        patterns.add(make("Indian Bank", "INDBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("Indian Bank UPI", "INBUPI",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|VPA|paid to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*)(\\d{10,20})"
        ));

        // ── Indian Overseas Bank ───────────────────────────────────────────────
        patterns.add(make("IOB",         "IOBBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|for)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        // ── Karur Vysya Bank ─────────────────────────────────────────────────
        patterns.add(make("KVB",         "KVBANK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("KVB UPI",     "KVBUPI",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*)(\\d{10,20})"
        ));

        // ── South Indian Bank ─────────────────────────────────────────────────
        patterns.add(make("SIB",         "SIBBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        // ── Federal Bank ──────────────────────────────────────────────────────
        patterns.add(make("Federal Bank", "FEDBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA|paid to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        // ── Tamilnad Mercantile Bank ──────────────────────────────────────────
        patterns.add(make("TMB",         "TMBBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        // ── City Union Bank ───────────────────────────────────────────────────
        patterns.add(make("CUB",         "CUBLTD",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("CUB UPI",     "CUBUPI",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|UPI|paid)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*)(\\d{10,20})"
        ));

        // ── HDFC Bank ─────────────────────────────────────────────────────────
        patterns.add(make("HDFC Bank",   "HDFCBK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to|@)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|spent|paid|withdrawn|purchase|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("HDFC Bank",   "HDFCBN",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|credited|credit|paid)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));
        patterns.add(make("HDFC Credit", "HDFCCC",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|spent|paid|purchase|credited|credit|refund)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{6,20})"
        ));

        // ── ICICI Bank ────────────────────────────────────────────────────────
        patterns.add(make("ICICI Bank",  "ICICIB",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|purchase|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("ICICI Credit","ICICIC",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:at|to)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|spent|purchase|used|credited|refund)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{6,20})"
        ));

        // ── Axis Bank ─────────────────────────────────────────────────────────
        patterns.add(make("Axis Bank",   "AXISBK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|paid to|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|spent|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));
        patterns.add(make("Axis Bank",   "AXISB",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI)",
            "(?:Ref\\.?\\s*No\\.?\\s*)(\\d{8,20})"
        ));

        // ── Kotak Bank ────────────────────────────────────────────────────────
        patterns.add(make("Kotak Bank",  "KOTAKB",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|for|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        // ── Bank of Baroda ────────────────────────────────────────────────────
        patterns.add(make("Bank of Baroda", "BOBBNK",
            "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)",
            "(?:to|at|VPA)\\s+([A-Za-z0-9 &'.@_-]{3,40})(?:\\s|$|\\.|,)",
            "(?i)(debited|debit|paid|UPI|credited|credit|received)",
            "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})"
        ));

        dao.insertAll(patterns);
    }

    private static SmsPattern make(String bank, String senderId,
                                    String amountRegex, String merchantRegex,
                                    String typeRegex,   String refRegex) {
        SmsPattern p = new SmsPattern();
        p.bankName      = bank;
        p.smsSenderId   = senderId;
        p.amountRegex   = amountRegex;
        p.merchantRegex = merchantRegex;
        p.typeRegex     = typeRegex;
        p.refRegex      = refRegex;
        p.isEnabled     = true;
        return p;
    }
}
