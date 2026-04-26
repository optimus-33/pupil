package com.pupil.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pupil.app.core.data.local.entity.PaymentAppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAppConfigDao {
    @Query("SELECT * FROM payment_app_configs ORDER BY paymentType, displayName")
    fun getAll(): Flow<List<PaymentAppConfigEntity>>

    @Query("SELECT * FROM payment_app_configs WHERE paymentType = :paymentType AND enabled = 1 ORDER BY displayName")
    fun getByType(paymentType: String): Flow<List<PaymentAppConfigEntity>>

    @Update
    suspend fun update(config: PaymentAppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: PaymentAppConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<PaymentAppConfigEntity>)
}
