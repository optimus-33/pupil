package com.pupil.app.core.data.repository

import com.pupil.app.core.data.local.dao.PaymentAppConfigDao
import com.pupil.app.core.data.local.entity.PaymentAppConfigEntity
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.repository.PaymentAppConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaymentAppConfigRepositoryImpl(
    private val dao: PaymentAppConfigDao
) : PaymentAppConfigRepository {
    override fun getPaymentApps(): Flow<List<PaymentAppConfig>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getPaymentAppsByType(paymentType: PaymentType): Flow<List<PaymentAppConfig>> =
        dao.getByType(paymentType.typeName).map { entities -> entities.map { it.toDomain() } }

    override suspend fun setAppEnabled(id: Long, enabled: Boolean) {
        dao.updateEnabled(id, enabled)
    }

    override suspend fun addPaymentAppConfig(config: PaymentAppConfig) {
        dao.insert(config.toEntity())
    }
}

private fun PaymentAppConfigEntity.toDomain() = PaymentAppConfig(
    id = id,
    displayName = displayName,
    packageName = packageName,
    paymentType = PaymentType.fromTypeName(paymentType),
    enabled = enabled
)

private fun PaymentAppConfig.toEntity() = PaymentAppConfigEntity(
    id = id,
    displayName = displayName,
    packageName = packageName,
    paymentType = paymentType.typeName,
    enabled = enabled
)
