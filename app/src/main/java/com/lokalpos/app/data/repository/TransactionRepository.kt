package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.TransactionDao
import com.lokalpos.app.data.dao.DailySales
import com.lokalpos.app.data.dao.PaymentMethodSummary
import com.lokalpos.app.data.entity.Transaction
import com.lokalpos.app.data.entity.TransactionItem
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAll()

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRange(startTime, endTime)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getById(id)

    suspend fun getTransactionItems(transactionId: Long): List<TransactionItem> =
        transactionDao.getItemsByTransaction(transactionId)

    suspend fun createTransaction(transaction: Transaction, items: List<TransactionItem>): Long =
        transactionDao.insertTransactionWithItems(transaction, items)

    suspend fun refundTransaction(id: Long) = transactionDao.updateStatus(id, "refunded")

    suspend fun deleteOldTransactions(beforeTimestamp: Long): Int =
        transactionDao.deleteOlderThan(beforeTimestamp)

    suspend fun generateReceiptNumber(): String {
        val maxNum = transactionDao.getMaxReceiptNumber() ?: 0
        return "TRX-%05d".format(maxNum + 1)
    }

    suspend fun getTotalSales(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalSales(startTime, endTime)

    suspend fun getTransactionCount(startTime: Long, endTime: Long): Int =
        transactionDao.getTransactionCount(startTime, endTime)

    suspend fun getAverageSale(startTime: Long, endTime: Long): Double =
        transactionDao.getAverageSale(startTime, endTime)

    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySales> =
        transactionDao.getDailySales(startTime, endTime)

    suspend fun getPaymentMethodSummary(startTime: Long, endTime: Long): List<PaymentMethodSummary> =
        transactionDao.getPaymentMethodSummary(startTime, endTime)
}
