package com.lokalpos.app.ui.screens.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.entity.Category
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val allProductsCount: Int = 0,
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val showCategoryDialog: Boolean = false,
    val editingCategory: Category? = null
)

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val repo = app.productRepository
    val settings = app.settingsManager

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllProducts().collect { products ->
                _uiState.update { state ->
                    // Only update allProductsCount when viewing all products (no filter)
                    if (state.selectedCategoryId == null && state.searchQuery.isBlank()) {
                        state.copy(products = products, allProductsCount = products.size)
                    } else {
                        state.copy(products = products)
                    }
                }
            }
        }
        viewModelScope.launch {
            repo.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        // Initialize allProductsCount
        viewModelScope.launch {
            repo.getAllProducts().collect { products ->
                _uiState.update { it.copy(allProductsCount = products.size) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                repo.getAllProducts().collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            } else {
                repo.searchProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            }
        }
    }

    fun filterByCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            val flow = if (categoryId != null) repo.getProductsByCategory(categoryId)
            else repo.getAllProducts()
            flow.collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch { repo.deleteProduct(productId) }
    }

    fun showCategoryDialog(category: Category? = null) {
        _uiState.update { it.copy(showCategoryDialog = true, editingCategory = category) }
    }

    fun hideCategoryDialog() {
        _uiState.update { it.copy(showCategoryDialog = false, editingCategory = null) }
    }

    fun saveCategory(name: String, color: String) {
        viewModelScope.launch {
            val existing = _uiState.value.editingCategory
            if (existing != null) {
                repo.updateCategory(existing.copy(name = name, color = color))
            } else {
                repo.insertCategory(Category(name = name, color = color))
            }
            hideCategoryDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repo.deleteCategory(category) }
    }
}
