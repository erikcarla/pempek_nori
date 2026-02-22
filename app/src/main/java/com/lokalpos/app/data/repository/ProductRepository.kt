package com.lokalpos.app.data.repository

import com.lokalpos.app.data.dao.CategoryDao
import com.lokalpos.app.data.dao.ProductDao
import com.lokalpos.app.data.entity.Category
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) {
    fun getAllProducts(): Flow<List<Product>> = productDao.getAll()

    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> =
        productDao.getByCategory(categoryId)

    fun searchProducts(query: String): Flow<List<Product>> = productDao.search(query)

    suspend fun getProductById(id: Long): Product? = productDao.getById(id)

    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getByBarcode(barcode)

    suspend fun insertProduct(product: Product): Long = productDao.insert(product)

    suspend fun updateProduct(product: Product) = productDao.update(product)

    suspend fun deleteProduct(id: Long) = productDao.softDelete(id)

    suspend fun decreaseStock(productId: Long, quantity: Int) =
        productDao.decreaseStock(productId, quantity)

    suspend fun increaseStock(productId: Long, quantity: Int) =
        productDao.increaseStock(productId, quantity)

    fun getLowStockProducts(): Flow<List<Product>> = productDao.getLowStock()

    suspend fun getTopSellingProducts(limit: Int = 10): List<Product> =
        productDao.getTopSelling(limit)

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllSync()

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getById(id)

    suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)
}
