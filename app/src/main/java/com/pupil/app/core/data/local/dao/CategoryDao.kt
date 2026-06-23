package com.pupil.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE transactionType = :transactionType AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getByTransactionType(transactionType: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE transactionType = :transactionType AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getByTransactionTypeOnce(transactionType: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :name, icon = :icon, color = :color, sortOrder = :sortOrder, isActive = :isActive WHERE id = :id")
    suspend fun update(id: Long, name: String, icon: String?, color: Int?, sortOrder: Int, isActive: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE isSystem = 0")
    suspend fun deleteAllNonSystem()
}
