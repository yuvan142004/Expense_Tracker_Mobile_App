package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.Transaction;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // All transactions ordered newest first
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getAllTransactions();

    // Transactions filtered by search query (merchant name or note)
    @Query("SELECT * FROM transactions WHERE " +
           "(merchantName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') " +
           "ORDER BY timestamp DESC")
    LiveData<List<Transaction>> searchTransactions(String query);

    // Transactions filtered by category
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getByCategory(int categoryId);

    // Transactions in a date range
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getByDateRange(long from, long to);

    // Transactions by category in a date range (for budget checking)
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId " +
           "AND transactionType = 'DEBIT' AND timestamp BETWEEN :from AND :to")
    double getSumByCategoryAndRange(int categoryId, long from, long to);

    // Today's total debit
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'DEBIT' " +
           "AND timestamp BETWEEN :startOfDay AND :endOfDay")
    double getTodayTotal(long startOfDay, long endOfDay);

    // This week's total debit
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'DEBIT' " +
           "AND timestamp BETWEEN :startOfWeek AND :endOfWeek")
    double getWeekTotal(long startOfWeek, long endOfWeek);

    // Monthly total per category (for pie chart)
    @Query("SELECT categoryId, SUM(amount) as total FROM transactions " +
           "WHERE transactionType = 'DEBIT' AND timestamp BETWEEN :from AND :to " +
           "GROUP BY categoryId")
    List<CategorySum> getMonthlyCategoryTotals(long from, long to);

    // Monthly totals for last N months (for bar chart)
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'DEBIT' " +
           "AND timestamp BETWEEN :from AND :to")
    double getTotalForPeriod(long from, long to);

    // Daily totals for a month (for line chart)
    @Query("SELECT * FROM transactions WHERE transactionType = 'DEBIT' " +
           "AND timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    List<Transaction> getTransactionsForPeriodSync(long from, long to);

    // ── New queries for UPI payment tracking ──────────────────────────────────

    /** Get all pending transactions (ordered by newest first) */
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    List<Transaction> getPendingTransactions();

    /** Get a transaction by ID */
    @Query("SELECT * FROM transactions WHERE id = :txnId LIMIT 1")
    Transaction getById(long txnId);

    /** Find pending transactions by amount and time range (for duplicate detection) */
    @Query("SELECT * FROM transactions WHERE " +
           "ABS(amount - :amount) < 0.01 AND " +
           "timestamp BETWEEN :startTime AND :endTime AND " +
           "status = 'PENDING'")
    List<Transaction> findPendingByAmountAndTime(double amount, long startTime, long endTime);

    // ──────────────────────────────────────────────────────────────────────────

    // ── Combined filter query ─────────────────────────────────────────────────
    // All parameters are optional — pass nulls / 0 / Long.MIN_VALUE to skip each filter.
    // :query        — merchant name or note (empty string = no text filter)
    // :categoryId   — 0 = any category
    // :type         — null = any type ("DEBIT" / "CREDIT")
    // :minAmount    — 0 = no minimum
    // :maxAmount    — Double.MAX_VALUE = no maximum
    // :fromDate     — 0 = no start bound
    // :toDate       — Long.MAX_VALUE = no end bound
    @Query("SELECT * FROM transactions WHERE " +
           "(:query = '' OR merchantName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') " +
           "AND (:categoryId = 0 OR categoryId = :categoryId) " +
           "AND (:type IS NULL OR transactionType = :type) " +
           "AND amount >= :minAmount " +
           "AND amount <= :maxAmount " +
           "AND timestamp >= :fromDate " +
           "AND timestamp <= :toDate " +
           "ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getFiltered(
            String query,
            int categoryId,
            String type,
            double minAmount,
            double maxAmount,
            long fromDate,
            long toDate);

    // Helper class for category sum query
    class CategorySum {
        public int categoryId;
        public double total;
    }
}
