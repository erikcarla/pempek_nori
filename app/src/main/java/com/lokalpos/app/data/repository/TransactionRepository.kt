package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.TransactionDao
import com.lokalpos.app.data.dao.AggregatedSalesItem
import com.lokalpos.app.data.dao.DailySales
import com.lokalpos.app.data.dao.PaymentMethodSummary
import com.lokalpos.app.data.entity.Transaction
import com.lokalpos.app.data.entity.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAll().flowOn(Dispatchers.IO)

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startTime, endTime).flowOn(Dispatchers.IO)

    suspend fun getTransactionById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getById(id)
    }

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItem> =
        withContext(Dispatchers.IO) {
            transactionDao.getItemsByTransaction(transactionId)
        }

    suspend fun createTransaction(transaction: Transaction, items: List<TransactionItem>): Long =
        withContext(Dispatchers.IO) {
            transactionDao.insertTransactionWithItems(transaction, items)
        }

    suspend fun refundTransaction(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.updateStatus(id, "refunded")
    }

    suspend fun deleteOldTransactions(beforeTimestamp: Long): Int = withContext(Dispatchers.IO) {
        transactionDao.deleteOlderThan(beforeTimestamp)
    }

    suspend fun generateReceiptNumber(): String = withContext(Dispatchers.IO) {
        val maxNum = transactionDao.getMaxReceiptNumber() ?: 0
        "TRX-%05d".format(maxNum + 1)
    }

    suspend fun getTotalSales(startTime: Long, endTime: Long): Double =
        withContext(Dispatchers.IO) { transactionDao.getTotalSales(startTime, endTime) }

    suspend fun getTransactionCount(startTime: Long, endTime: Long): Int =
        withContext(Dispatchers.IO) { transactionDao.getTransactionCount(startTime, endTime) }

    suspend fun getAverageSale(startTime: Long, endTime: Long): Double =
        withContext(Dispatchers.IO) { transactionDao.getAverageSale(startTime, endTime) }

    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySales> =
        withContext(Dispatchers.IO) { transactionDao.getDailySales(startTime, endTime) }

    suspend fun getPaymentMethodSummary(startTime: Long, endTime: Long): List<PaymentMethodSummary> =
        withContext(Dispatchers.IO) {
            transactionDao.getPaymentMethodSummary(startTime, endTime)
        }

    suspend fun getAggregatedSalesItems(startTime: Long, endTime: Long): List<AggregatedSalesItem> =
        withContext(Dispatchers.IO) {
            transactionDao.getAggregatedSalesItems(startTime, endTime)
        }
}
