package com.pupil.app.core.domain.model

enum class TransactionType(val typeName: String) {
    EXPENSE("EXPENSE"),
    INCOME("INCOME"),
    REFUND("REFUND"),
    TRANSFER("TRANSFER");

    companion object {
        fun fromTypeName(value: String?): TransactionType =
            values().firstOrNull { it.typeName == value } ?: EXPENSE
    }
}
