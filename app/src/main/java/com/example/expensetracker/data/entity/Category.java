package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;       // e.g. "Food", "Shopping"

    public String colorHex;   // e.g. "#FF6B6B"

    public String iconRes;    // drawable resource name e.g. "ic_food"

    public boolean isDefault; // true = system default, false = user-created
}
