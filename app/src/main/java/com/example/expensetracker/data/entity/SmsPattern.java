package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_patterns")
public class SmsPattern {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String bankName;       // e.g. "HDFC"

    public String smsSenderId;    // e.g. "HDFCBK" — matches SMS sender

    public String amountRegex;    // Regex to extract transaction amount

    public String merchantRegex;  // Regex to extract merchant/payee name

    public String typeRegex;      // Regex to detect DEBIT or CREDIT

    public String refRegex;       // Regex to extract UPI reference ID

    public boolean isEnabled;
}
