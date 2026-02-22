package com.lokalpos.app.data.dao

import androidx.room.*
import com.lokalpos.app.data.entity.Transaction
import com.lokalpos.app.data.entity.TransactionItem
import kotlinx.coroutines.flow.Flow

data class DailySales(
    val date: String,
    val total: Double,
    val count: Int
)

data class PaymentMethodSummary(
    val paymentMethod: String,
    val total: Double,
    val count: Int
)

data class CategorySales(
    val categoryName: String,
    val total: Double,
    val count: Int
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsByTransaction(transactionId: Long): List<TransactionItem>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert
    suspend fun insertItems(items: List<TransactionItem>)

    @androidx.room.Transaction
    suspend fun insertTransactionWithItems(transaction: Transaction, items: List<TransactionItem>): Long {
        val txId = insertTransaction(transaction)
        val itemsWithTxId = items.map { it.copy(transactionId = txId) }
        insertItems(itemsWithTxId)
        return txId
    }

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM transactions WHERE createdAt < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'completed'")
    suspend fun countCompleted(): Int

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM transactions WHERE status = 'completed' AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getTotalSales(startTime: Long, endTime: Long): Double

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'completed' AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getTransactionCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COALESCE(AVG(totalAmount), 0) FROM transactions WHERE status = 'completed' AND createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getAverageSale(startTime: Long, endTime: Long): Double

    @Query("""
        SELECT 
            strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') as date,
            SUM(totalAmount) as total,
            COUNT(*) as count
        FROM transactions 
        WHERE status = 'completed' AND createdAt >= :startTime AND createdAt <= :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySales>

    @Query("""
        SELECT paymentMethod, SUM(totalAmount) as total, COUNT(*) as count
        FROM transactions 
        WHERE status = 'completed' AND createdAt >= :startTime AND createdAt <= :endTime
        GROUP BY paymentMethod
        ORDER BY total DESC
    """)
    suspend fun getPaymentMethodSummary(startTime: Long, endTime: Long): List<PaymentMethodSummary>

    @Query("SELECT MAX(CAST(REPLACE(receiptNumber, 'TRX-', '') AS INTEGER)) FROM transactions")
    suspend fun getMaxReceiptNumber(): Int?
}
