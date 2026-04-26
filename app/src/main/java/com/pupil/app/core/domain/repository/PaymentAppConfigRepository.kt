package com.pupil.app.core.domain.repository

import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import kotlinx.coroutines.flow.Flow

interface PaymentAppConfigRepository {
    fun getPaymentApps(): Flow<List<PaymentAppConfig>>
    fun getPaymentAppsByType(paymentType: PaymentType): Flow<List<PaymentAppConfig>>
    suspend fun setAppEnabled(id: Long, enabled: Boolean)
    suspend fun addPaymentAppConfig(config: PaymentAppConfig)
}
