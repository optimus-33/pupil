package com.pupil.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_app_configs")
data class PaymentAppConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val packageName: String,
    val paymentType: String,
    val enabled: Boolean = true
)
