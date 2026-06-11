package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.BankAccount;
import java.util.List;

@Dao
public interface BankAccountDao {

    @Insert
    long insert(BankAccount account);

    @Update
    void update(BankAccount account);

    @Delete
    void delete(BankAccount account);

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId")
    LiveData<List<BankAccount>> getAccountsForUser(int userId);

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId")
    List<BankAccount> getAccountsForUserSync(int userId);

    @Query("SELECT smsSenderId FROM bank_accounts WHERE userId = :userId")
    List<String> getSenderIdsForUser(int userId);
}
