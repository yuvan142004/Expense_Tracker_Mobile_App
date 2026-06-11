package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.BudgetLimit;
import java.util.List;

@Dao
public interface BudgetLimitDao {

    @Insert
    long insert(BudgetLimit limit);

    @Update
    void update(BudgetLimit limit);

    @Delete
    void delete(BudgetLimit limit);

    @Query("SELECT * FROM budget_limits ORDER BY categoryId ASC")
    LiveData<List<BudgetLimit>> getAllLimits();

    @Query("SELECT * FROM budget_limits WHERE isActive = 1")
    List<BudgetLimit> getActiveLimitsSync();

    @Query("SELECT * FROM budget_limits WHERE categoryId = :categoryId LIMIT 1")
    BudgetLimit getLimitByCategorySync(int categoryId);
}
