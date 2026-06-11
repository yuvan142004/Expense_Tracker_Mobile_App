package com.example.expensetracker.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.BudgetLimit;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.utils.NotificationHelper;

import java.util.Calendar;
import java.util.List;

public class BudgetCheckWorker extends Worker {

    public BudgetCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<BudgetLimit> activeLimits = db.budgetLimitDao().getActiveLimitsSync();

        for (BudgetLimit limit : activeLimits) {
            long[] range = getDateRange(limit);
            if (range == null) continue;

            double spent = db.transactionDao()
                    .getSumByCategoryAndRange(limit.categoryId, range[0], range[1]);

            if (spent >= limit.limitAmount) {
                Category cat = db.categoryDao().getCategoryByIdSync(limit.categoryId);
                String catName = cat != null ? cat.name : "Category";
                NotificationHelper.showBudgetAlert(
                        getApplicationContext(), catName, spent, limit.limitAmount);
            }
        }
        return Result.success();
    }

    /** Returns [fromMs, toMs] for a given BudgetLimit period. */
    private long[] getDateRange(BudgetLimit limit) {
        switch (limit.period) {
            case "MONTHLY": {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long from = cal.getTimeInMillis();
                cal.add(Calendar.MONTH, 1);
                return new long[]{from, cal.getTimeInMillis()};
            }
            case "WEEKLY": {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long from = cal.getTimeInMillis();
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                return new long[]{from, cal.getTimeInMillis()};
            }
            case "CUSTOM": {
                if (limit.fromDate != null && limit.toDate != null) {
                    return new long[]{limit.fromDate, limit.toDate};
                }
                return null;
            }
            default:
                return null;
        }
    }
}
