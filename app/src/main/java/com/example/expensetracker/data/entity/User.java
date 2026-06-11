package com.example.expensetracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public String pinHash; // SHA-256 hashed PIN, nullable

    public boolean useBiometric;

    @androidx.annotation.Nullable
    public String profilePicturePath; // absolute path to saved profile image, nullable
}
