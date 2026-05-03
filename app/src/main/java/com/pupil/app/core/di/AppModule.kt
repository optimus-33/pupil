package com.pupil.app.core.di

import android.content.Context
import com.pupil.app.core.data.local.PupilDatabase
import com.pupil.app.core.data.local.dao.AccountDao
import com.pupil.app.core.data.local.dao.CategoryDao
import com.pupil.app.core.data.local.dao.PaymentAppConfigDao
import com.pupil.app.core.data.local.dao.TagDao
import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.repository.PaymentAppConfigRepositoryImpl
import com.pupil.app.core.data.repository.TransactionRepositoryImpl
import com.pupil.app.core.domain.repository.PaymentAppConfigRepository
import com.pupil.app.core.domain.repository.TransactionRepository
import com.pupil.app.core.domain.usecase.AddPaymentAppConfigUseCase
import com.pupil.app.core.domain.usecase.GetAllPaymentAppsUseCase
import com.pupil.app.core.domain.usecase.GetAllTransactionsUseCase
import com.pupil.app.core.domain.usecase.GetCategoryTotalsInRangeUseCase
import com.pupil.app.core.domain.usecase.GetPaymentAppsByTypeUseCase
import com.pupil.app.core.domain.usecase.GetPendingTransactionsUseCase
import com.pupil.app.core.domain.usecase.GetTotalInRangeUseCase
import com.pupil.app.core.domain.usecase.GetTotalInRangeByTypeUseCase
import com.pupil.app.core.domain.usecase.GetTransactionByIdUseCase
import com.pupil.app.core.domain.usecase.GetTransactionCountUseCase
import com.pupil.app.core.domain.usecase.GetTransactionsInRangeUseCase
import com.pupil.app.core.domain.usecase.InsertTransactionsUseCase
import com.pupil.app.core.domain.usecase.SaveTransactionUseCase
import com.pupil.app.core.domain.usecase.SetPaymentAppEnabledUseCase
import com.pupil.app.core.domain.usecase.SoftDeleteTransactionUseCase
import com.pupil.app.core.domain.usecase.TransactionUseCases
import com.pupil.app.core.domain.usecase.UpdateTransactionStatusUseCase
import com.pupil.app.core.domain.usecase.UpdateTransactionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PupilDatabase = PupilDatabase.buildDatabase(context)

    @Provides
    fun provideTransactionDao(database: PupilDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun providePaymentAppConfigDao(database: PupilDatabase): PaymentAppConfigDao = database.paymentAppConfigDao()

    @Provides
    fun provideCategoryDao(database: PupilDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideAccountDao(database: PupilDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideTagDao(database: PupilDatabase): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao, categoryDao: CategoryDao): TransactionRepository =
        TransactionRepositoryImpl(dao, categoryDao)

    @Provides
    @Singleton
    fun providePaymentAppConfigRepository(dao: PaymentAppConfigDao): PaymentAppConfigRepository =
        PaymentAppConfigRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTransactionUseCases(
        transactionRepository: TransactionRepository,
        paymentAppConfigRepository: PaymentAppConfigRepository
    ): TransactionUseCases = TransactionUseCases(
        getAllTransactions = GetAllTransactionsUseCase(transactionRepository),
        getTodayTotal = GetTotalInRangeUseCase(transactionRepository),
        getWeeklyTotal = GetTotalInRangeUseCase(transactionRepository),
        getMonthlyTotal = GetTotalInRangeUseCase(transactionRepository),
        getCategoryTotalsInRange = GetCategoryTotalsInRangeUseCase(transactionRepository),
        saveTransaction = SaveTransactionUseCase(transactionRepository),
        deleteTransaction = SoftDeleteTransactionUseCase(transactionRepository),
        updateTransactionStatus = UpdateTransactionStatusUseCase(transactionRepository),
        updateTransaction = UpdateTransactionUseCase(transactionRepository),
        getTransactionById = GetTransactionByIdUseCase(transactionRepository),
        getPendingTransactions = GetPendingTransactionsUseCase(transactionRepository),
        getPaymentAppsByType = GetPaymentAppsByTypeUseCase(paymentAppConfigRepository),
        getAllPaymentApps = GetAllPaymentAppsUseCase(paymentAppConfigRepository),
        setPaymentAppEnabled = SetPaymentAppEnabledUseCase(paymentAppConfigRepository),
        addPaymentAppConfig = AddPaymentAppConfigUseCase(paymentAppConfigRepository),
        getTransactionsInRange = GetTransactionsInRangeUseCase(transactionRepository),
        getTransactionCount = GetTransactionCountUseCase(transactionRepository),
        insertTransactions = InsertTransactionsUseCase(transactionRepository),
        getTotalInRangeByType = GetTotalInRangeByTypeUseCase(transactionRepository)
    )
}

