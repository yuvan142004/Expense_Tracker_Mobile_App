package com.example.expensetracker;

import android.app.Application;

import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.utils.AppThemeHelper;
import com.example.expensetracker.utils.NotificationHelper;

public class ExpenseTrackerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set night-mode BEFORE any Activity starts so DayNight theme resolves correctly
        AppThemeHelper.applyNightMode(this);

        // Initialize notification channels
        NotificationHelper.createChannels(this);

        // Trigger DB instantiation (pre-populate runs here on first launch)
        AppDatabase.getInstance(this);
    }
}
