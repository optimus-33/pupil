package com.pupil.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val category: String,
    val paymentType: String,
    val paymentApp: String,
    val timestamp: Long,
    val isManual: Boolean,
    val status: String = "COMPLETED"
)

