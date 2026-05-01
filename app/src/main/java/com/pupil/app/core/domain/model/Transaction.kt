package com.pupil.app.core.domain.model

data class Transaction(
    val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val category: String,
    val paymentType: PaymentType,
    val paymentApp: String,
    val timestamp: Long,
    val isManual: Boolean = false,
    val status: TransactionStatus = TransactionStatus.COMPLETED
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

