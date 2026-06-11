package com.example.expensetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.expensetracker.data.entity.Category;
import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    List<Category> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE id = :id")
    Category getCategoryByIdSync(int id);

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    Category getCategoryByName(String name);

    @Query("SELECT COUNT(*) FROM categories")
    int getCount();
}
