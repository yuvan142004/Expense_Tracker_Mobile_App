package com.example.expensetracker.data.dao;

import androidx.room.*;
import com.example.expensetracker.data.entity.SmsPattern;
import java.util.List;

@Dao
public interface SmsPatternDao {

    // ── Write ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(SmsPattern pattern);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<SmsPattern> patterns);

    @Update
    void update(SmsPattern pattern);

    @Delete
    void delete(SmsPattern pattern);

    // ── Read (original method names — keep unchanged so SmsReceiver compiles) ─

    /** All enabled patterns. Used by SmsReceiver for fast pre-filter. */
    @Query("SELECT * FROM sms_patterns WHERE isEnabled = 1")
    List<SmsPattern> getEnabledPatterns();

    /**
     * Exact sender-ID lookup (case-insensitive).
     * Used by SmsReceiver to find the best pattern for a matched sender.
     *
     * The sender arriving in SmsReceiver is already stripped of its prefix
     * (e.g. "SBIINB" not "AD-SBIINB") — see SenderIdExtractor.strip().
     */
    @Query("SELECT * FROM sms_patterns " +
           "WHERE UPPER(smsSenderId) = UPPER(:senderId) AND isEnabled = 1 LIMIT 1")
    SmsPattern getPatternBySenderId(String senderId);

    /** Row count — used by AppDatabase pre-populate callback. */
    @Query("SELECT COUNT(*) FROM sms_patterns")
    int getCount();

    // ── New helper ────────────────────────────────────────────────────────────

    /**
     * Used by Step2BankFragment to avoid inserting duplicate pattern rows.
     * Returns all rows (enabled or not) for a given sender ID.
     */
    @Query("SELECT * FROM sms_patterns WHERE UPPER(smsSenderId) = UPPER(:senderId)")
    List<SmsPattern> getPatternsForSender(String senderId);
}
