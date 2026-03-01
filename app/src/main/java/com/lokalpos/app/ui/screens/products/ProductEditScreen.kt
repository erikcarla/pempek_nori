package com.lokalpos.app.ui.screens.products

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val color: String? = null,
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
                        color = product.color,
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
                color = s.color
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

            // Color picker
            Text("Warna Produk", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colors = listOf(
                    null to "Default",
                    "#F44336" to "Merah",
                    "#E91E63" to "Pink",
                    "#9C27B0" to "Ungu",
                    "#3F51B5" to "Biru",
                    "#03A9F4" to "Biru Muda",
                    "#009688" to "Teal",
                    "#4CAF50" to "Hijau",
                    "#FF9800" to "Orange",
                    "#795548" to "Coklat"
                )
                colors.forEach { (colorHex, _) ->
                    val isSelected = state.color == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (colorHex != null) {
                                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorHex))
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                }
                            )
                            .clickable { viewModel.updateField { copy(color = colorHex) } }
                    )
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
