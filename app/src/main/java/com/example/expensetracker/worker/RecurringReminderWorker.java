package com.example.expensetracker.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.RecurringExpense;
import com.example.expensetracker.utils.NotificationHelper;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecurringReminderWorker extends Worker {

    // Remind if due within 2 days
    private static final long TWO_DAYS_MS = TimeUnit.DAYS.toMillis(2);

    public RecurringReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        long threshold = System.currentTimeMillis() + TWO_DAYS_MS;

        List<RecurringExpense> dueItems = db.recurringExpenseDao().getDueRecurringSync(threshold);

        for (RecurringExpense item : dueItems) {
            NotificationHelper.showRecurringReminder(
                    getApplicationContext(), item.name, item.amount);

            // Roll forward nextDueDate
            item.nextDueDate = rollForward(item.nextDueDate, item.frequency);
            db.recurringExpenseDao().update(item);
        }

        return Result.success();
    }

    private long rollForward(long currentDue, String frequency) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentDue);
        if ("WEEKLY".equals(frequency)) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        } else {
            cal.add(Calendar.MONTH, 1);
        }
        return cal.getTimeInMillis();
    }
}
