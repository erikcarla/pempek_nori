package com.lokalpos.app.ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lokalpos.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    navController: NavController,
    viewModel: PosViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = viewModel.settings
    var showBarcodeInput by remember { mutableStateOf(false) }
    var barcodeText by remember { mutableStateOf("") }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }

    // Error snackbar
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            viewModel.dismissError()
        }
    }

    if (state.showSuccess) {
        PaymentSuccessDialog(
            transaction = state.completedTransaction,
            settings = settings,
            onPrint = { viewModel.printLastReceipt() },
            onDismiss = { viewModel.dismissSuccess() }
        )
    }

    if (showDiscountDialog) {
        DiscountDialog(
            currentPercent = state.discountPercent,
            currentAmount = state.discountAmount,
            onApply = { pct, amt -> viewModel.setDiscount(pct, amt); showDiscountDialog = false },
            onDismiss = { showDiscountDialog = false }
        )
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            onSelect = { viewModel.selectCustomer(it); showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Kasir") },
            actions = {
                IconButton(onClick = { showBarcodeInput = !showBarcodeInput }) {
                    Icon(Icons.Filled.QrCodeScanner, "Scan Barcode")
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Filled.Settings, "Pengaturan")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Barcode input
        AnimatedVisibility(visible = showBarcodeInput) {
            OutlinedTextField(
                value = barcodeText,
                onValueChange = { barcodeText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                label = { Text("Scan / Ketik Barcode") },
                trailingIcon = {
                    IconButton(onClick = {
                        if (barcodeText.isNotBlank()) {
                            viewModel.addByBarcode(barcodeText.trim())
                            barcodeText = ""
                        }
                    }) {
                        Icon(Icons.Filled.Search, "Cari")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }

        // Category chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("Semua") }
                )
            }
            items(state.categories) { category ->
                FilterChip(
                    selected = state.selectedCategoryId == category.id,
                    onClick = { viewModel.selectCategory(category.id) },
                    label = { Text(category.name) }
                )
            }
        }

        // Search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.searchProducts(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            placeholder = { Text("Cari produk...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.searchProducts("") }) {
                        Icon(Icons.Filled.Close, "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Product grid + Cart
        if (state.showCheckout) {
            CheckoutView(
                state = state,
                settings = settings,
                onBack = { viewModel.toggleCheckout() },
                onPaymentMethodChange = { viewModel.setPaymentMethod(it) },
                onAmountPaidChange = { viewModel.setAmountPaid(it) },
                onPay = { viewModel.processPayment() }
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Product grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductGridItem(
                            product = product,
                            currencySymbol = settings.currencySymbol,
                            onClick = { viewModel.addToCart(product) }
                        )
                    }
                }

                // Cart summary bar
                if (state.cart.isNotEmpty()) {
                    CartSummaryBar(
                        state = state,
                        settings = settings,
                        onShowCart = { viewModel.toggleCheckout() },
                        onShowDiscount = { showDiscountDialog = true },
                        onShowCustomer = { showCustomerPicker = true },
                        onClear = { viewModel.clearCart() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGridItem(
    product: com.lokalpos.app.data.entity.Product,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$currencySymbol %,.0f".format(product.price),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            if (product.trackStock && product.inStock <= product.lowStockAlert) {
                Text(
                    text = "Stok: ${product.inStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CartSummaryBar(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    onShowCart: () -> Unit,
    onShowDiscount: () -> Unit,
    onShowCustomer: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cart items summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${state.cartItemCount} item",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.selectedCustomer != null) {
                        Text(
                            state.selectedCustomer.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    settings.formatCurrency(state.total),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onShowDiscount,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.LocalOffer, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Diskon", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onShowCustomer,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Plgn", fontSize = 12.sp)
                }
                Button(
                    onClick = onShowCart,
                    modifier = Modifier.weight(1.5f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Bayar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CheckoutView(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    onBack: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onPay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Kembali")
            }
            Text("Pembayaran", style = MaterialTheme.typography.titleLarge)
        }

        // Cart items list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.cart) { cartItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cartItem.product.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${cartItem.quantity} x ${settings.formatCurrency(cartItem.product.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            settings.formatCurrency(cartItem.subtotal),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Totals
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal")
            Text(settings.formatCurrency(state.subtotal))
        }
        if (state.discountTotal > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diskon", color = SuccessGreen)
                Text("-${settings.formatCurrency(state.discountTotal)}", color = SuccessGreen)
            }
        }
        if (state.taxAmount > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pajak (${state.taxPercent.toInt()}%)")
                Text(settings.formatCurrency(state.taxAmount))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                settings.formatCurrency(state.total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment method
        Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.paymentMethods.toList()) { method ->
                FilterChip(
                    selected = state.paymentMethod == method,
                    onClick = { onPaymentMethodChange(method) },
                    label = { Text(method) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Amount paid (only for cash)
        if (state.paymentMethod == "Tunai") {
            OutlinedTextField(
                value = state.amountPaid,
                onValueChange = onAmountPaidChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jumlah Dibayar") },
                prefix = { Text("${settings.currencySymbol} ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            if (state.changeAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Kembalian: ${settings.formatCurrency(state.changeAmount)}",
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            // Quick amount buttons
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickAmounts = listOf(
                    state.total,
                    ((state.total / 10000).toInt() + 1) * 10000.0,
                    ((state.total / 50000).toInt() + 1) * 50000.0,
                    ((state.total / 100000).toInt() + 1) * 100000.0
                ).distinct().take(4)
                quickAmounts.forEach { amount ->
                    OutlinedButton(
                        onClick = { onAmountPaidChange("%,.0f".format(amount).replace(",", "")) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("%,.0f".format(amount), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.canPay && !state.isProcessing,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Payment, null)
                Spacer(Modifier.width(8.dp))
                Text("BAYAR ${settings.formatCurrency(state.total)}", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PaymentSuccessDialog(
    transaction: com.lokalpos.app.data.entity.Transaction?,
    settings: com.lokalpos.app.util.SettingsManager,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    if (transaction == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Pembayaran Berhasil!", textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No: ${transaction.receiptNumber}")
                Text(
                    settings.formatCurrency(transaction.totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (transaction.changeAmount > 0) {
                    Text("Kembalian: ${settings.formatCurrency(transaction.changeAmount)}")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrint) {
                    Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak Struk")
                }
                Button(onClick = onDismiss) {
                    Text("Selesai")
                }
            }
        }
    )
}

@Composable
private fun DiscountDialog(
    currentPercent: Double,
    currentAmount: Double,
    onApply: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var isPercent by remember { mutableStateOf(currentPercent > 0) }
    var value by remember {
        mutableStateOf(
            if (currentPercent > 0) currentPercent.toString()
            else if (currentAmount > 0) currentAmount.toString()
            else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diskon") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPercent,
                        onClick = { isPercent = true },
                        label = { Text("Persen (%)") }
                    )
                    FilterChip(
                        selected = !isPercent,
                        onClick = { isPercent = false },
                        label = { Text("Nominal (Rp)") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isPercent) "Persen Diskon" else "Nominal Diskon") },
                    suffix = { Text(if (isPercent) "%" else "") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = value.toDoubleOrNull() ?: 0.0
                if (isPercent) onApply(v, 0.0) else onApply(0.0, v)
            }) { Text("Terapkan") }
        },
        dismissButton = {
            TextButton(onClick = {
                onApply(0.0, 0.0)
            }) { Text("Hapus Diskon") }
        }
    )
}

@Composable
private fun CustomerPickerDialog(
    customers: List<com.lokalpos.app.data.entity.Customer>,
    onSelect: (com.lokalpos.app.data.entity.Customer?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = customers.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) ||
                (it.phone?.contains(searchQuery) == true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari pelanggan...") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(filtered) { customer ->
                        ListItem(
                            headlineContent = { Text(customer.name) },
                            supportingContent = {
                                Text(customer.phone ?: customer.email ?: "")
                            },
                            modifier = Modifier.clickable { onSelect(customer) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) { Text("Tanpa Pelanggan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
