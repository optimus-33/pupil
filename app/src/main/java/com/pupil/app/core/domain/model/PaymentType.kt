package com.pupil.app.core.domain.model

enum class PaymentType(val typeName: String) {
    UPI("UPI"),
    UPI_CREDIT_CARD("UPI_CREDIT_CARD");

    companion object {
        fun fromTypeName(value: String?): PaymentType = values().firstOrNull { it.typeName == value } ?: UPI
    }
}
