package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "recurring_expenses",
    foreignKeys = @ForeignKey(
        entity = Category.class,
        parentColumns = "id",
        childColumns = "categoryId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("categoryId")}
)
public class RecurringExpense {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;          // e.g. "Netflix", "Rent"

    public double amount;

    public int categoryId;       // FK → Category

    /** Frequency: "MONTHLY", "WEEKLY" */
    public String frequency;

    public long nextDueDate;     // Unix ms — next reminder date

    public boolean isActive;
}
