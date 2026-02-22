package com.lokalpos.app

import android.app.Application
import com.lokalpos.app.data.AppDatabase
import com.lokalpos.app.data.repository.CustomerRepository
import com.lokalpos.app.data.repository.ProductRepository
import com.lokalpos.app.data.repository.TransactionRepository
import com.lokalpos.app.util.SettingsManager
import com.lokalpos.app.worker.TransactionCleanupWorker

class LokalPosApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var productRepository: ProductRepository
        private set
    lateinit var transactionRepository: TransactionRepository
        private set
    lateinit var customerRepository: CustomerRepository
        private set
    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        productRepository = ProductRepository(database.productDao(), database.categoryDao())
        transactionRepository = TransactionRepository(database.transactionDao())
        customerRepository = CustomerRepository(database.customerDao())
        settingsManager = SettingsManager(this)

        TransactionCleanupWorker.schedule(this)
    }
}
