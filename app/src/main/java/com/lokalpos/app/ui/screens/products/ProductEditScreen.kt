package com.lokalpos.app.ui.screens.products

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.entity.Category
import com.lokalpos.app.data.entity.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductEditState(
    val name: String = "",
    val price: String = "",
    val cost: String = "",
    val barcode: String = "",
    val sku: String = "",
    val categoryId: Long? = null,
    val trackStock: Boolean = false,
    val inStock: String = "0",
    val lowStockAlert: String = "5",
    val color: String = "#4CAF50",
    val shape: String = "circle",
    val categories: List<Category> = emptyList(),
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class ProductEditViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val repo = app.productRepository

    private val _state = MutableStateFlow(ProductEditState())
    val state: StateFlow<ProductEditState> = _state.asStateFlow()

    private var productId: Long = -1L

    fun loadProduct(id: Long) {
        productId = id
        viewModelScope.launch {
            repo.getAllCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
        if (id > 0) {
            viewModelScope.launch {
                val product = repo.getProductById(id) ?: return@launch
                _state.update {
                    it.copy(
                        name = product.name,
                        price = product.price.toLong().toString(),
                        cost = if (product.cost > 0) product.cost.toLong().toString() else "",
                        barcode = product.barcode ?: "",
                        sku = product.sku ?: "",
                        categoryId = product.categoryId,
                        trackStock = product.trackStock,
                        inStock = product.inStock.toString(),
                        lowStockAlert = product.lowStockAlert.toString(),
                        color = product.color ?: "#4CAF50",
                        shape = product.shape ?: "circle",
                        isNew = false
                    )
                }
            }
        }
    }

    fun updateField(updater: ProductEditState.() -> ProductEditState) {
        _state.update { it.updater() }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.price.isBlank()) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val product = Product(
                id = if (productId > 0) productId else 0,
                name = s.name.trim(),
                price = s.price.toDoubleOrNull() ?: 0.0,
                cost = s.cost.toDoubleOrNull() ?: 0.0,
                barcode = s.barcode.ifBlank { null },
                sku = s.sku.ifBlank { null },
                categoryId = s.categoryId,
                trackStock = s.trackStock,
                inStock = s.inStock.toIntOrNull() ?: 0,
                lowStockAlert = s.lowStockAlert.toIntOrNull() ?: 5,
                color = s.color.ifBlank { null },
                shape = s.shape.ifBlank { null }
            )

            if (s.isNew) {
                repo.insertProduct(product)
            } else {
                repo.updateProduct(product)
            }

            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    productId: Long,
    onBack: () -> Unit,
    viewModel: ProductEditViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCategoryDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Tambah Produk" else "Edit Produk") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> viewModel.updateField { copy(name = v) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Produk *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.price,
                    onValueChange = { v -> viewModel.updateField { copy(price = v) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Harga Jual *") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.cost,
                    onValueChange = { v -> viewModel.updateField { copy(cost = v) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Harga Modal") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category selector
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = state.categories.find { it.id == state.categoryId }?.name ?: "Tanpa Kategori",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tanpa Kategori") },
                        onClick = {
                            viewModel.updateField { copy(categoryId = null) }
                            showCategoryDropdown = false
                        }
                    )
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.updateField { copy(categoryId = category.id) }
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.barcode,
                    onValueChange = { v -> viewModel.updateField { copy(barcode = v) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Barcode") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = state.sku,
                    onValueChange = { v -> viewModel.updateField { copy(sku = v) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("SKU") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider()

            Text("Warna", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0", "#00BCD4", "#FF5722", "#607D8B").forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray })
                            .clickable { viewModel.updateField { copy(color = hex) } },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        if (state.color == hex) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Bentuk", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("circle" to "Bulat", "square" to "Kotak").forEach { (value, label) ->
                    FilterChip(
                        selected = state.shape == value,
                        onClick = { viewModel.updateField { copy(shape = value) } },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider()

            // Stock management
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column {
                    Text("Lacak Stok", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Aktifkan untuk melacak stok barang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.trackStock,
                    onCheckedChange = { v -> viewModel.updateField { copy(trackStock = v) } }
                )
            }

            if (state.trackStock) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.inStock,
                        onValueChange = { v -> viewModel.updateField { copy(inStock = v) } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Jumlah Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = state.lowStockAlert,
                        onValueChange = { v -> viewModel.updateField { copy(lowStockAlert = v) } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Peringatan Stok Rendah") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.name.isNotBlank() && state.price.isNotBlank() && !state.isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isNew) "Simpan Produk" else "Update Produk")
                }
            }
        }
    }
}
