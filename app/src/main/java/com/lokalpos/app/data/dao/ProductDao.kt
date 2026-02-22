package com.lokalpos.app.data.dao

import androidx.room.*
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND categoryId = :categoryId ORDER BY name ASC")
    fun getByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%') ORDER BY name ASC")
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE products SET inStock = inStock - :quantity, soldCount = soldCount + :quantity WHERE id = :id AND trackStock = 1")
    suspend fun decreaseStock(id: Long, quantity: Int)

    @Query("UPDATE products SET inStock = inStock + :quantity WHERE id = :id AND trackStock = 1")
    suspend fun increaseStock(id: Long, quantity: Int)

    @Query("SELECT * FROM products WHERE trackStock = 1 AND inStock <= lowStockAlert AND isActive = 1 ORDER BY inStock ASC")
    fun getLowStock(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY soldCount DESC LIMIT :limit")
    suspend fun getTopSelling(limit: Int = 10): List<Product>
}
