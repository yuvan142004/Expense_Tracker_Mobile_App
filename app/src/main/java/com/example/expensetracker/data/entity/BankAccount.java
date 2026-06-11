package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "bank_accounts",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId")}
)
public class BankAccount {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String bankName;       // e.g. "HDFC", "SBI"

    public String last4Digits;    // e.g. "1234" — matches "a/c XX1234" in SMS

    public String smsSenderId;    // e.g. "HDFCBK", "SBIINB"

    public int userId;            // FK → User

    /** Enable SMS tracking for this account (default: true) */
    public boolean enableSmsTracking;

    /** Enable manual UPI logging for this account (default: false for RELIABLE banks) */
    public boolean enableUpiTracking;
}
