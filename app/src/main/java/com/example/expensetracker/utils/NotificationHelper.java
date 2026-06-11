package com.example.expensetracker.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.expensetracker.R;
import com.example.expensetracker.data.entity.Transaction;
import com.example.expensetracker.ui.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_BUDGET   = "budget_alerts";
    public static final String CHANNEL_RECURRING = "recurring_reminders";
    public static final String CHANNEL_SMS       = "sms_transaction";
    public static final String CHANNEL_DUPLICATE = "duplicate_detection";

    private NotificationHelper() {}

    /** Create all notification channels (call once on app start, API 26+). */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                context.getSystemService(NotificationManager.class);

            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ));

            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_RECURRING,
                "Recurring Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ));

            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_SMS,
                "SMS Transactions",
                NotificationManager.IMPORTANCE_LOW
            ));

            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_DUPLICATE,
                "Duplicate Detection",
                NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }

    /** Show a budget exceeded notification. */
    public static void showBudgetAlert(Context context, String categoryName,
                                       double spent, double limit) {
        String title = "Budget Alert: " + categoryName;
        String body  = String.format("You've spent \u20B9%.0f of your \u20B9%.0f limit.", spent, limit);
        showNotification(context, CHANNEL_BUDGET, (int) System.currentTimeMillis(), title, body);
    }

    /** Show a recurring expense reminder. */
    public static void showRecurringReminder(Context context, String name, double amount) {
        String title = "Upcoming: " + name;
        String body  = String.format("\u20B9%.0f is due soon. Tap to review.", amount);
        showNotification(context, CHANNEL_RECURRING, name.hashCode(), title, body);
    }

    /** Show a silent SMS transaction detected notification. */
    public static void showSmsTransaction(Context context, String merchant, double amount,
                                          String type) {
        String title = type.equals("DEBIT") ? "Expense Detected" : "Credit Detected";
        String body  = String.format("%s: \u20B9%.0f via %s",
                type.equals("DEBIT") ? "Spent" : "Received", amount,
                merchant != null ? merchant : "Bank SMS");
        showNotification(context, CHANNEL_SMS, (int) System.currentTimeMillis(), title, body);
    }

    /**
     * Show SMS transaction notification with pending match warning
     * This is used when an SMS transaction matches a pending manual UPI transaction
     */
    public static void showSmsTransactionWithPendingMatch(Context context, String merchant, 
                                                          double amount, String type, 
                                                          Transaction pendingTxn) {
        String title = "Possible Duplicate Detected!";
        String pendingMerchant = pendingTxn.merchantName != null ? 
                pendingTxn.merchantName : "Unknown";
        String pendingApp = pendingTxn.upiAppUsed != null ? 
                pendingTxn.upiAppUsed : "UPI app";
        
        String body = String.format(
                "SMS: \u20B9%.0f via %s\n\n" +
                "You have a PENDING manual transaction:\n" +
                "• Merchant: %s\n" +
                "• Amount: \u20B9%.0f\n" +
                "• App: %s\n\n" +
                "This might be the same transaction. " +
                "Both have been saved separately. You can manually merge them later.",
                amount,
                merchant != null ? merchant : "Bank SMS",
                pendingMerchant,
                pendingTxn.amount,
                pendingApp
        );

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("pending_txn_id", pendingTxn.id);
        intent.putExtra("show_merge_suggestion", true);
        
        PendingIntent pi = PendingIntent.getActivity(context, 
                (int) pendingTxn.id, // Use txn ID for unique pending intent
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_DUPLICATE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("SMS transaction matches pending manual entry")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        try {
            NotificationManagerCompat.from(context).notify(
                    (int) (System.currentTimeMillis() / 1000), // Unique ID
                    builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS permission not granted — fail silently
        }
    }

    private static void showNotification(Context context, String channel,
                                         int notifId, String title, String body) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS permission not granted — fail silently
        }
    }
}
