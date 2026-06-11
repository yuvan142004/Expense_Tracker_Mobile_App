package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.User;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users LIMIT 1")
    User getUser();

    @Query("SELECT * FROM users LIMIT 1")
    LiveData<User> getUserLive();

    @Query("UPDATE users SET name = :name WHERE id = :id")
    void updateName(int id, String name);

    @Query("UPDATE users SET profilePicturePath = :path WHERE id = :id")
    void updateProfilePicture(int id, String path);

    @Query("DELETE FROM users")
    void deleteAll();
}
