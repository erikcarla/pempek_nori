package com.lokalpos.app.data.dao

import androidx.room.*
import com.lokalpos.app.data.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("UPDATE customers SET totalSpent = totalSpent + :amount, visitCount = visitCount + 1, loyaltyPoints = loyaltyPoints + :points WHERE id = :id")
    suspend fun addPurchase(id: Long, amount: Double, points: Int)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int
}
