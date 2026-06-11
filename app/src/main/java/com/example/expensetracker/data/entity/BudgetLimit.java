package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "budget_limits",
    foreignKeys = @ForeignKey(
        entity = Category.class,
        parentColumns = "id",
        childColumns = "categoryId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("categoryId")}
)
public class BudgetLimit {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categoryId;       // FK → Category

    public double limitAmount;

    /** Period type: "MONTHLY", "WEEKLY", "CUSTOM" */
    public String period;

    public Long fromDate;        // nullable — Unix ms, used when period = CUSTOM

    public Long toDate;          // nullable — Unix ms, used when period = CUSTOM

    public boolean isActive;     // user can pause without deleting
}
