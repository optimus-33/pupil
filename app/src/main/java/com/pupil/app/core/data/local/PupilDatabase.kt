package com.pupil.app.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pupil.app.core.data.local.dao.PaymentAppConfigDao
import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.local.entity.PaymentAppConfigEntity
import com.pupil.app.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransactionEntity::class, PaymentAppConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PupilDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentAppConfigDao(): PaymentAppConfigDao

    companion object {
        fun buildDatabase(context: Context): PupilDatabase {
            lateinit var database: PupilDatabase
            val callback = object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        database.paymentAppConfigDao().insertAll(
                            listOf(
                                PaymentAppConfigEntity(displayName = "Google Pay", packageName = "com.google.android.apps.nbu.paisa.user", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "PhonePe", packageName = "com.phonepe.app", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "Paytm", packageName = "net.one97.paytm", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "HDFC Bank", packageName = "com.snapwork.hdfc", paymentType = "UPI_CREDIT_CARD"),
                                PaymentAppConfigEntity(displayName = "Axis Mobile", packageName = "com.axis.mobile", paymentType = "UPI_CREDIT_CARD"),
                                PaymentAppConfigEntity(displayName = "ICICI iMobile", packageName = "com.csam.icici.bank.imobile", paymentType = "UPI_CREDIT_CARD")
                            )
                        )
                    }
                }
            }
            database = Room.databaseBuilder(context, PupilDatabase::class.java, "pupil_db")
                .addCallback(callback)
                .fallbackToDestructiveMigration()
                .build()
            return database
        }
    }
}
