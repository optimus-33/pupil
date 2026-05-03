package com.pupil.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,              // SAVINGS | CREDIT_CARD | WALLET | CASH
    val institutionName: String?,
    val accountSuffix: String?,    // Last 4 digits
    val cardLastFour: String?,
    val currency: String = "INR",
    val openingBalance: Long = 0,  // In paise
    val creditLimit: Long?,        // For credit cards
    val icon: String?,
    val color: Int?,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
