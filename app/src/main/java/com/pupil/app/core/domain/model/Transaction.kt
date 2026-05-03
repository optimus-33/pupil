package com.pupil.app.core.domain.model

data class Transaction(
    val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val notes: String? = null,
    val categoryId: Long,
    val categoryName: String = "Other",
    val categoryIcon: String? = null,
    val categoryColor: Int? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val paymentType: PaymentType,
    val paymentApp: String,
    val merchantCode: String? = null,
    val referenceNumber: String? = null,
    val accountId: Long? = null,
    val timestamp: Long,
    val isManual: Boolean = false,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

enum class TransactionStatus(val statusName: String) {
    PENDING("PENDING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromStatusName(value: String?): TransactionStatus =
            values().firstOrNull { it.statusName == value } ?: COMPLETED
    }
}
