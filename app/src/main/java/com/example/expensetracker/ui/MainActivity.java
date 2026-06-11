package com.example.expensetracker.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.work.*;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.Transaction;
import com.example.expensetracker.ui.add.AddBottomSheet;
import com.example.expensetracker.utils.AppPreferences;
import com.example.expensetracker.utils.AppThemeHelper;
import com.example.expensetracker.worker.BudgetCheckWorker;
import com.example.expensetracker.worker.RecurringReminderWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppThemeHelper.apply(this); // BEFORE super.onCreate() — palette + font overlays
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Navigation Component with Bottom Nav
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Intercept the "Add" placeholder — it is NOT a navigation destination
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.addPlaceholder) {
                new AddBottomSheet().show(getSupportFragmentManager(), "AddBottomSheet");
                return false; // don't update selection state
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        // Schedule WorkManager jobs
        scheduleWorkers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if there's a pending transaction to confirm
        checkPendingTransactionConfirmation();
    }

    /**
     * Check if user has a pending UPI transaction and ask for confirmation
     */
    private void checkPendingTransactionConfirmation() {
        long pendingTxnId = AppPreferences.getPendingTransactionId(this);
        
        if (pendingTxnId == -1) {
            return; // No pending transaction
        }
        
        // Load transaction from database
        AppDatabase.dbExecutor.execute(() -> {
            Transaction txn = AppDatabase.getInstance(this)
                    .transactionDao().getById(pendingTxnId);
            
            if (txn == null || !"PENDING".equals(txn.status)) {
                // Transaction not found or already processed
                runOnUiThread(() -> AppPreferences.clearPendingTransactionId(this));
                return;
            }
            
            runOnUiThread(() -> showPaymentConfirmationDialog(txn));
        });
    }

    /**
     * Show dialog asking user if they completed the payment
     */
    private void showPaymentConfirmationDialog(Transaction txn) {
        String merchant = txn.merchantName != null ? txn.merchantName : "Unknown Merchant";
        String amount = "₹" + String.format("%.2f", txn.amount);
        String upiApp = txn.upiAppUsed != null ? txn.upiAppUsed : "UPI app";
        
        String message = "Did you complete the payment?\n\n" +
                "Merchant: " + merchant + "\n" +
                "Amount: " + amount + "\n" +
                "App: " + upiApp;
        
        new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage(message)
                .setPositiveButton("Yes, Completed", (dialog, which) -> {
                    markTransactionAsCompleted(txn.id);
                })
                .setNegativeButton("No, Cancel Payment", (dialog, which) -> {
                    markTransactionAsCancelled(txn.id);
                })
                .setNeutralButton("Ask Later", (dialog, which) -> {
                    // Keep transaction as pending, will ask again next time
                })
                .setCancelable(false) // Force user to make a choice
                .show();
    }

    /**
     * Mark transaction as COMPLETED
     */
    private void markTransactionAsCompleted(long txnId) {
        AppDatabase.dbExecutor.execute(() -> {
            Transaction txn = AppDatabase.getInstance(this).transactionDao().getById(txnId);
            
            if (txn != null) {
                txn.status = "COMPLETED";
                AppDatabase.getInstance(this).transactionDao().update(txn);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Payment marked as completed", Toast.LENGTH_SHORT).show();
                    AppPreferences.clearPendingTransactionId(this);
                    refreshHistoryFragment();
                });
            }
        });
    }

    /**
     * Mark transaction as CANCELLED and delete it
     */
    private void markTransactionAsCancelled(long txnId) {
        AppDatabase.dbExecutor.execute(() -> {
            Transaction txn = AppDatabase.getInstance(this).transactionDao().getById(txnId);
            
            if (txn != null) {
                AppDatabase.getInstance(this).transactionDao().delete(txn);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Transaction cancelled and removed", Toast.LENGTH_SHORT).show();
                    AppPreferences.clearPendingTransactionId(this);
                    refreshHistoryFragment();
                });
            }
        });
    }

    /**
     * Refresh the history fragment to show updated transactions
     */
    private void refreshHistoryFragment() {
        // Get the NavHostFragment and current fragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        
        if (navHostFragment != null) {
            // The fragment will auto-refresh if it observes LiveData
            // If not, you can trigger a manual refresh here
            // For now, we rely on the HistoryFragment's LiveData observer
        }
    }

    private void scheduleWorkers() {
        WorkManager wm = WorkManager.getInstance(this);

        // Budget check every 6 hours
        PeriodicWorkRequest budgetWork = new PeriodicWorkRequest.Builder(
                BudgetCheckWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        wm.enqueueUniquePeriodicWork(
                "budget_check",
                ExistingPeriodicWorkPolicy.KEEP,
                budgetWork);

        // Recurring reminder check once daily
        PeriodicWorkRequest recurringWork = new PeriodicWorkRequest.Builder(
                RecurringReminderWorker.class, 1, TimeUnit.DAYS)
                .build();
        wm.enqueueUniquePeriodicWork(
                "recurring_check",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringWork);
    }
}
