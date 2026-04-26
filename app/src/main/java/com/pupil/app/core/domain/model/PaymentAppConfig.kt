package com.pupil.app.core.domain.model

data class PaymentAppConfig(
    val id: Long = 0,
    val displayName: String,
    val packageName: String,
    val paymentType: PaymentType,
    val enabled: Boolean = true
)
