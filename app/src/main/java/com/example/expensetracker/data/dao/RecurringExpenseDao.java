package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.RecurringExpense;
import java.util.List;

@Dao
public interface RecurringExpenseDao {

    @Insert
    long insert(RecurringExpense expense);

    @Update
    void update(RecurringExpense expense);

    @Delete
    void delete(RecurringExpense expense);

    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDate ASC")
    LiveData<List<RecurringExpense>> getAllRecurring();

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND nextDueDate <= :thresholdMs")
    List<RecurringExpense> getDueRecurringSync(long thresholdMs);
}
