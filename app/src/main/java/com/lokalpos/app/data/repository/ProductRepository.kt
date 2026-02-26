package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.CategoryDao
import com.lokalpos.app.data.dao.ProductDao
import com.lokalpos.app.data.entity.Category
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) {
    fun getAllProducts(): Flow<List<Product>> = productDao.getAll().flowOn(Dispatchers.IO)

    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> =
        productDao.getByCategory(categoryId).flowOn(Dispatchers.IO)

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.search(query).flowOn(Dispatchers.IO)

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        productDao.getById(id)
    }

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        productDao.getByBarcode(barcode)
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insert(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.update(product)
    }

    suspend fun deleteProduct(id: Long) = withContext(Dispatchers.IO) {
        productDao.softDelete(id)
    }

    suspend fun decreaseStock(productId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        productDao.decreaseStock(productId, quantity)
    }

    suspend fun increaseStock(productId: Long, quantity: Int) = withContext(Dispatchers.IO) {
        productDao.increaseStock(productId, quantity)
    }

    fun getLowStockProducts(): Flow<List<Product>> = productDao.getLowStock().flowOn(Dispatchers.IO)

    suspend fun getTopSellingProducts(limit: Int = 10): List<Product> = withContext(Dispatchers.IO) {
        productDao.getTopSelling(limit)
    }

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll().flowOn(Dispatchers.IO)

    suspend fun getAllCategoriesSync(): List<Category> = withContext(Dispatchers.IO) {
        categoryDao.getAllSync()
    }

    suspend fun getCategoryById(id: Long): Category? = withContext(Dispatchers.IO) {
        categoryDao.getById(id)
    }

    suspend fun insertCategory(category: Category): Long = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.delete(category)
    }
}
