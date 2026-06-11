# 💰 ExpenseTracker AI

> An intelligent Android expense tracker that **automatically reads your bank SMS messages**, classifies transactions using on-device AI, and gives you real-time spending insights — no manual entry required.


## ✨ Features

- **🔔 Automatic SMS Parsing** — Reads bank transaction SMSes in real time. Extracts amount, merchant name, UPI reference, and debit/credit type using regex patterns for 20+ Indian banks.
- **🤖 On-Device AI Classification** — Three TensorFlow Lite models run entirely on the phone (no internet needed): category classifier, anomaly detector, and spending predictor.
- **📊 Visual Spending Dashboard** — Pie, bar, and line charts powered by MPAndroidChart with date-range filters (week / month / year / custom).
- **🏦 UPI Payment Tracking** — Launch GPay, PhonePe, or any UPI app from within the app. Payments are saved as PENDING and auto-confirmed when the bank SMS arrives.
- **🔔 Budget Alerts** — Set monthly spending limits per category. WorkManager checks every 6 hours and sends a notification when you're close to or over the limit.
- **🔄 Recurring Expense Reminders** — Log subscriptions and monthly bills; get daily reminders before they're due.
- **🔒 Biometric / PIN Lock** — Protect the app with fingerprint, face unlock, or a 4-digit PIN using AndroidX BiometricPrompt.
- **🌙 Dark Mode Support** — Full light/dark theme switching via AppCompatDelegate.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 14 (API 34) |
| Database | Room 2.6 (SQLite ORM) |
| Navigation | AndroidX Navigation Component |
| Background Jobs | WorkManager 2.9 |
| Charts | MPAndroidChart v3.1 |
| AI / ML | TensorFlow Lite 2.17 |
| Camera | CameraX 1.3 |
| Barcode Scan | Google ML Kit 17.2 |
| Biometrics | AndroidX Biometric 1.1 |
| UI | Material Components 1.11 + ViewBinding |

---

## 🏗️ Project Structure

```
app/src/main/java/com/example/expensetracker/
│
├── data/
│   ├── entity/               # Room @Entity classes (database tables)
│   │   ├── Transaction.java  # Core data model — 14 fields
│   │   ├── Category.java
│   │   ├── BankAccount.java
│   │   ├── BudgetLimit.java
│   │   ├── RecurringExpense.java
│   │   ├── SmsPattern.java   # Per-bank regex rules
│   │   └── User.java
│   ├── dao/                  # Room @Dao interfaces (all SQL queries)
│   │   ├── TransactionDao.java
│   │   ├── CategoryDao.java
│   │   ├── BankAccountDao.java
│   │   ├── BudgetLimitDao.java
│   │   ├── RecurringExpenseDao.java
│   │   ├── SmsPatternDao.java
│   │   └── UserDao.java
│   ├── AppDatabase.java      # Singleton Room DB, 4 migrations
│   ├── BankSenderRegistry.java
│   └── BankSenderInfo.java
│
├── ml/                       # On-device AI models
│   ├── ExpenseClassifier.java     # Keyword-based category classifier
│   ├── AnomalyDetector.java       # TFLite — detects unusual daily spending
│   ├── SpendingPredictor.java     # TFLite — predicts next month's budget
│   └── SmartCategoryClassifier.java  # TFLite — reclassifies "Other" expenses
│
├── receiver/
│   └── SmsReceiver.java      # BroadcastReceiver — entry point for all bank SMSes
│
├── worker/
│   ├── BudgetCheckWorker.java       # Runs every 6 hours
│   └── RecurringReminderWorker.java # Runs daily
│
├── utils/
│   ├── SmsParser.java         # Regex engine — parses amount, merchant, type, UPI ref
│   ├── SenderIdExtractor.java # Strips TRAI prefix from sender IDs
│   ├── AppPreferences.java    # SharedPreferences wrapper
│   ├── BiometricHelper.java
│   ├── PinHelper.java
│   ├── NotificationHelper.java
│   ├── UpiHelper.java
│   ├── UpiAppLauncher.java
│   ├── UpiAppDetector.java
│   ├── AppThemeHelper.java
│   └── SpendingInsightsAnalyzer.java
│
└── ui/
    ├── MainActivity.java
    ├── SplashActivity.java
    ├── home/         HomeFragment + CategoryExpenseAdapter
    ├── history/      HistoryFragment + TransactionAdapter + FilterBottomSheet
    ├── alerts/       AlertsFragment + BudgetLimitAdapter + RecurringExpenseAdapter
    ├── add/          AddBottomSheet + ManualEntryDialog + UpiAppSelectorDialog
    ├── profile/      ProfileFragment + SettingsFragment + ProfileContainerFragment
    ├── lock/         LockActivity
    └── onboarding/   OnboardingActivity + 4 step fragments
```

---

## ⚙️ How It Works

### SMS Auto-Parsing Pipeline

```
Bank SMS arrives
      │
      ▼
SmsReceiver.onReceive()      ← BroadcastReceiver (priority 999)
      │
      ▼
SenderIdExtractor            ← Strips TRAI prefix  (AD-SBIINB → SBIINB)
      │
      ▼
BankSenderRegistry           ← Match sender to a registered bank
      │
      ▼
SmsParser (Regex)            ← Extract: amount · merchant · type · UPI ref
      │
      ▼
ExpenseClassifier            ← Keyword map → assign category
      │
      ▼
Duplicate check              ← Find PENDING transaction (±5 min, same amount)
      │
      ▼
AppDatabase.dbExecutor       ← Save on background thread (4-thread pool)
      │
      ▼
NotificationHelper           ← Post transaction notification
```

### AI Models (all on-device, no internet)

| Model | Input | Output |
|---|---|---|
| `ExpenseClassifier` | Merchant name keywords | Category (Food, Transport, etc.) |
| `AnomalyDetector.tflite` | [daily_amount, day_of_week, day_of_month] | Anomaly probability 0–1 |
| `SpendingPredictor.tflite` | Recent monthly totals | Next month's predicted spend |
| `SmartCategoryClassifier.tflite` | Transaction features | Refined category for "Other" |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android device or emulator running API 24+
- JDK 17

### Clone and Build

```bash
git clone https://github.com/YOUR_USERNAME/ExpenseTracker.git
cd ExpenseTracker
```

Open in Android Studio → **File → Open** → select the project folder.

Let Gradle sync complete, then click **Run ▶**.

### Permissions Required

The app requests the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `RECEIVE_SMS` / `READ_SMS` | Auto-parse bank transaction messages |
| `CAMERA` | QR code scanning for UPI payments |
| `POST_NOTIFICATIONS` | Budget alerts and transaction notifications |
| `USE_BIOMETRIC` | Fingerprint / face lock |

> ⚠️ SMS permission is required for the core auto-parsing feature. Without it, you can still add transactions manually.

---

## 🗄️ Database Schema

The app uses **Room** with 7 tables and 4 schema migrations:

```
users ──────────────────────────────────────────────┐
bank_accounts ──────────────────────────────────────┤
categories ─────────────────────────────────────────┤
transactions (categoryId FK, bankAccountId FK) ──────┘
budget_limits
recurring_expenses
sms_patterns
```

**Migration history:**
- `v1 → v2` — Added `profilePicturePath` to users
- `v2 → v3` — Added `status` (COMPLETED / PENDING / CANCELLED) to transactions
- `v3 → v4` — Added `paymentMethod` (CASH / UPI / CARD / NET_BANKING) to transactions

---

## 🔑 Key Design Decisions

**Why `int` vs `Integer` for foreign keys?**
`categoryId` is `int` (primitive, never null — defaults to 10 = "Other"). `bankAccountId` is `Integer` (nullable — cash payments have no bank account).

**Why `aaptOptions { noCompress 'tflite' }`?**
TFLite models are loaded via `MappedByteBuffer` (memory-mapped file access). Compressed assets can't be memory-mapped, so the `.tflite` files must be stored uncompressed in the APK.

**Why WorkManager over background threads?**
WorkManager survives app kills, device reboots, and Doze mode. A plain `Thread` or `Timer` would stop when the app closes.

**Why `onDelete = SET_DEFAULT` for category FK?**
Deleting a category reassigns its transactions to "Other" (id=10) instead of deleting them (CASCADE) or blocking the deletion (RESTRICT).

---

## 📦 Dependencies

```groovy
// Database
implementation 'androidx.room:room-runtime:2.6.1'

// Navigation
implementation 'androidx.navigation:navigation-fragment:2.7.6'

// Background
implementation 'androidx.work:work-runtime:2.9.0'

// Charts
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// AI
implementation 'org.tensorflow:tensorflow-lite:2.17.0'

// Camera + QR
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'com.google.mlkit:barcode-scanning:17.2.0'

// Biometric
implementation 'androidx.biometric:biometric:1.1.0'

// Material UI
implementation 'com.google.android.material:material:1.11.0'
```

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Yuvarajan**
- GitHub: [yuvan142004](https://github.com/yuvan142004)

---

> Built as a learning project to demonstrate Android development with Room, Navigation, WorkManager, TFLite, CameraX, and Biometric APIs.
