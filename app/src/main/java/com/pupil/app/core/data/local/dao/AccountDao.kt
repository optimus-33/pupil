package com.pupil.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.core.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getAllActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Query("""
        UPDATE accounts SET
        name = :name, type = :type, institutionName = :institutionName,
        accountSuffix = :accountSuffix, cardLastFour = :cardLastFour,
        openingBalance = :openingBalance, creditLimit = :creditLimit,
        icon = :icon, color = :color, isActive = :isActive,
        sortOrder = :sortOrder, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun update(
        id: Long,
        name: String,
        type: String,
        institutionName: String?,
        accountSuffix: String?,
        cardLastFour: String?,
        openingBalance: Long,
        creditLimit: Long?,
        icon: String?,
        color: Int?,
        isActive: Boolean,
        sortOrder: Int,
        updatedAt: Long
    )

    @Query("UPDATE accounts SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM accounts")
    suspend fun hardDeleteAll()

    @Query("""
        SELECT IFNULL(SUM(CASE WHEN status = 'COMPLETED' AND transactionType IN ('INCOME', 'REFUND') THEN amountPaise ELSE 0 END), 0) -
               IFNULL(SUM(CASE WHEN status = 'COMPLETED' AND transactionType IN ('EXPENSE', 'TRANSFER') THEN amountPaise ELSE 0 END), 0)
        FROM transactions WHERE accountId = :accountId AND deletedAt IS NULL
    """)
    suspend fun getTransactionBalance(accountId: Long): Long
}
