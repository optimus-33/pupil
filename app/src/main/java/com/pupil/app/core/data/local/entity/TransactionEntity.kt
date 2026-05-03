package com.pupil.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"], orders = [Index.Order.DESC]),
        Index(value = ["status"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["transactionType"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val notes: String?,
    @ColumnInfo(name = "categoryId") val categoryId: Long,
    @ColumnInfo(name = "transactionType") val transactionType: String,
    val paymentType: String,
    val paymentApp: String,
    val merchantCode: String?,
    val referenceNumber: String?,
    @ColumnInfo(name = "accountId") val accountId: Long?,
    val timestamp: Long,
    val isManual: Boolean,
    val status: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
    @ColumnInfo(name = "deletedAt") val deletedAt: Long?
)

