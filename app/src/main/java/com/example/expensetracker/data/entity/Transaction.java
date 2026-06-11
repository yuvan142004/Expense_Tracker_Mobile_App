package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "transactions",
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "categoryId",
            onDelete = ForeignKey.SET_DEFAULT
        ),
        @ForeignKey(
            entity = BankAccount.class,
            parentColumns = "id",
            childColumns = "bankAccountId",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index("categoryId"),
        @Index("bankAccountId")
    }
)
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;

    public int categoryId;            // FK → Category (defaultValue = 10 = "Other")

    public Integer bankAccountId;     // FK → BankAccount (nullable — null for manual/cash)

    /** Source of the transaction: "SMS", "QR_UPI", "MANUAL" */
    public String source;

    /** Type of transaction: "DEBIT", "CREDIT" */
    public String transactionType;

    public String merchantName;       // nullable — extracted from SMS or QR

    public String upiRef;             // nullable — UPI transaction reference ID

    public String note;               // nullable — user-added personal note

    public String rawSms;             // nullable — original SMS body for debugging

    public boolean isEdited;          // true if user manually corrected the category

    public long timestamp;            // Unix timestamp in milliseconds

    /** Status: "COMPLETED", "PENDING", "CANCELLED" (default: "COMPLETED") */
    public String status;

    /** UPI app used for manual payments: "Google Pay", "PhonePe", etc. */
    public String upiAppUsed;

    /** Full QR data scanned (for QR payments): "upi://pay?pa=..." */
    public String scannedQrData;

    /** Payment method: "CASH", "UPI", "CARD", "NET_BANKING" */
    public String paymentMethod;
}
