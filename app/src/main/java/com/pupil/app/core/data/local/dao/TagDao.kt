package com.pupil.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.core.data.local.entity.TagEntity
import com.pupil.app.core.data.local.entity.TransactionTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllOnce(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToTransaction(transactionTag: TransactionTagEntity)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId AND tagId = :tagId")
    suspend fun removeTagFromTransaction(transactionId: Long, tagId: Long)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun clearTransactionTagsForTransaction(transactionId: Long)

    @Query("DELETE FROM transaction_tags")
    suspend fun clearAllTransactionTags()

    @Query("SELECT * FROM transaction_tags")
    suspend fun getAllTransactionTags(): List<TransactionTagEntity>

    @Query("SELECT tagId FROM transaction_tags WHERE transactionId = :transactionId")
    fun getTagIdsForTransaction(transactionId: Long): Flow<List<Long>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForTransaction(transactionId: Long): List<TagEntity>
}
