package com.pupil.app.core.domain.model

data class Category(
    val id: Long,
    val name: String,
    val icon: String? = null,
    val color: Int? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isSystem: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
