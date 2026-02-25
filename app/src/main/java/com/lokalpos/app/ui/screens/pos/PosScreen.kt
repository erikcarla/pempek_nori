package com.lokalpos.app.ui.screens.pos

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokalpos.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onOpenDrawer: () -> Unit,
    viewModel: PosViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = viewModel.settings
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 600

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

    if (state.showTicketDialog) {
        SaveTicketDialog(
            currentTicketName = state.currentTicketName,
            duplicateError = state.ticketDuplicateError,
            suggestedName = state.ticketSuggestedName,
            onSave = { viewModel.saveTicket(it) },
            onDismiss = { viewModel.hideTicketDialog(); viewModel.clearTicketError() }
        )
    }

    state.editingCartItem?.let { cartItem ->
        QuantityEditorDialog(
            cartItem = cartItem,
            settings = settings,
            onUpdate = { qty -> viewModel.updateCartQuantity(cartItem.product.id, qty) },
            onDismiss = { viewModel.hideQuantityEditor() }
        )
    }

    if (state.showTicketList) {
        TicketListDialog(
            tickets = state.openTickets,
            settings = settings,
            onLoad = { viewModel.loadTicket(it) },
            onDelete = { viewModel.deleteTicket(it) },
            onDismiss = { viewModel.hideTicketList() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.currentTicketName != null) "Kasir - ${state.currentTicketName}" else "Kasir"
                    )
                    if (state.openTickets.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("${state.openTickets.size}") }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, "Menu")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showTicketList() }) {
                    Icon(Icons.Filled.TableBar, "Open Tickets")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        if (isTablet) {
            TabletLayout(
                state = state,
                settings = settings,
                viewModel = viewModel
            )
        } else {
            PhoneLayout(
                state = state,
                settings = settings,
                viewModel = viewModel
            )
        }
    }
}

// ========================= TABLET LAYOUT =========================

@Composable
private fun TabletLayout(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: Products 70%
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
        ) {
            ProductPanel(state = state, settings = settings, viewModel = viewModel)
        }

        VerticalDivider()

        // Right: Cart 30%
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (state.showCheckout) {
                TabletCheckoutPanel(state = state, settings = settings, viewModel = viewModel)
            } else {
                TabletCartPanel(state = state, settings = settings, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun TabletCartPanel(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (state.currentTicketName != null) state.currentTicketName!! else "Keranjang",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (state.cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ShoppingCart, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Keranjang kosong",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.cart) { cartItem ->
                    CartItemRow(cartItem, settings, viewModel)
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(12.dp)) {
                if (state.taxAmount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "PB1 (${state.taxPercent.toInt()}%${if (state.taxInclusive) ", inc" else ""})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(settings.formatCurrency(state.taxAmount), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        settings.formatCurrency(state.total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.showTicketDialog() },
                        modifier = Modifier.weight(1f)
                    ) { Text("SIMPAN") }
                    Button(
                        onClick = { viewModel.toggleCheckout() },
                        modifier = Modifier.weight(1f)
                    ) { Text("BAYAR ${settings.formatCurrency(state.total)}") }
                }
            }
        }
    }
}

@Composable
private fun TabletCheckoutPanel(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.toggleCheckout() }) {
                Icon(Icons.Filled.ArrowBack, "Kembali")
            }
            Text("Pembayaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "TOTAL: ${settings.formatCurrency(state.total)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.paymentMethods.toList()) { method ->
                FilterChip(
                    selected = state.paymentMethod == method,
                    onClick = { viewModel.setPaymentMethod(method) },
                    label = { Text(method) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.paymentMethod == "Tunai") {
            CashPaymentInput(state, settings, viewModel)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.processPayment() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.canPay && !state.isProcessing,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Payment, null)
                Spacer(Modifier.width(8.dp))
                Text("BAYAR ${settings.formatCurrency(state.total)}", fontSize = 16.sp)
            }
        }
    }
}

// ========================= PHONE LAYOUT =========================

@Composable
private fun PhoneLayout(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    if (state.showCheckout) {
        PhoneCheckoutView(state = state, settings = settings, viewModel = viewModel)
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                ProductPanel(state = state, settings = settings, viewModel = viewModel)
            }

            if (state.cart.isNotEmpty()) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${state.cartItemCount} item", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                settings.formatCurrency(state.total),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.clearCart() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hapus", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.showTicketDialog() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.TableBar, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Simpan", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.toggleCheckout() },
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.ShoppingCart, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Bayar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneCheckoutView(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.toggleCheckout() }) {
                Icon(Icons.Filled.ArrowBack, "Kembali")
            }
            Text("Pembayaran", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.cart) { cartItem ->
                CartItemRow(cartItem, settings, viewModel)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                settings.formatCurrency(state.total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.paymentMethods.toList()) { method ->
                FilterChip(
                    selected = state.paymentMethod == method,
                    onClick = { viewModel.setPaymentMethod(method) },
                    label = { Text(method) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (state.paymentMethod == "Tunai") {
            CashPaymentInput(state, settings, viewModel)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.processPayment() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.canPay && !state.isProcessing,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Payment, null)
                Spacer(Modifier.width(8.dp))
                Text("BAYAR ${settings.formatCurrency(state.total)}", fontSize = 16.sp)
            }
        }
    }
}

// ========================= SHARED COMPONENTS =========================

@Composable
private fun ProductPanel(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category dropdown + Search in one row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.showSearchMode) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.searchProducts(it) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = { Text("Cari produk...", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSearchMode(); viewModel.searchProducts("") }) {
                            Icon(Icons.Filled.Close, "Tutup")
                        }
                    }
                )
            } else {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                IconButton(onClick = { viewModel.toggleSearchMode() }) {
                    Icon(Icons.Filled.Search, "Cari")
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Product grid or list
        if (settings.productDisplayMode == "list") {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductListItemCompact(
                        product = product,
                        settings = settings,
                        onClick = { viewModel.addToCart(product) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductGridItem(
                        product = product,
                        currencySymbol = settings.currencySymbol,
                        onClick = { viewModel.addToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductListItemCompact(
    product: com.lokalpos.app.data.entity.Product,
    settings: com.lokalpos.app.util.SettingsManager,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    settings.formatCurrency(product.price),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$currencySymbol ${"%,d".format(product.price.toLong()).replace(",", ".")}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            if (product.trackStock && product.inStock <= product.lowStockAlert) {
                Text(
                    text = "Stok: ${product.inStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.showQuantityEditor(cartItem) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cartItem.product.name,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${cartItem.quantity} x ${settings.formatCurrency(cartItem.product.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    settings.formatCurrency(cartItem.subtotal),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(
                onClick = { viewModel.removeFromCart(cartItem.product.id) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    "Hapus",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CashPaymentInput(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    OutlinedTextField(
        value = state.amountPaid,
        onValueChange = { viewModel.setAmountPaid(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Jumlah Dibayar") },
        prefix = { Text("${settings.currencySymbol} ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    if (state.changeAmount > 0) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Kembalian: ${settings.formatCurrency(state.changeAmount)}",
            color = SuccessGreen,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val quickAmounts = listOf(
            state.total,
            ((state.total / 10000).toInt() + 1) * 10000.0,
            ((state.total / 50000).toInt() + 1) * 50000.0,
            ((state.total / 100000).toInt() + 1) * 100000.0
        ).distinct().take(4)
        quickAmounts.forEach { amount ->
            val amtStr = "%,d".format(amount.toLong()).replace(",", ".")
            OutlinedButton(
                onClick = { viewModel.setAmountPaid(amtStr) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(amtStr, fontSize = 11.sp)
            }
        }
    }
}

// ========================= DIALOGS =========================

@Composable
private fun QuantityEditorDialog(
    cartItem: CartItem,
    settings: com.lokalpos.app.util.SettingsManager,
    onUpdate: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember(cartItem) { mutableStateOf(cartItem.quantity) }
    fun applyQty(newQty: Int) {
        val n = newQty.coerceIn(1, 9999)
        qty = n
        onUpdate(n)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(cartItem.product.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${cartItem.quantity} x ${settings.formatCurrency(cartItem.product.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    settings.formatCurrency(cartItem.product.price * qty),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = { applyQty(qty - 1) }) {
                        Icon(Icons.Filled.Remove, "Kurang")
                    }
                    OutlinedTextField(
                        value = qty.toString(),
                        onValueChange = { v ->
                            val n = v.toIntOrNull()
                            if (n != null && n in 1..9999) applyQty(n)
                        },
                        modifier = Modifier.widthIn(min = 64.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(onClick = { applyQty(qty + 1) }) {
                        Icon(Icons.Filled.Add, "Tambah")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Selesai") } }
    )
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
            Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPrint) {
                    Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cetak Struk")
                }
                Button(onClick = onDismiss) { Text("Selesai") }
            }
        }
    )
}

@Composable
private fun SaveTicketDialog(
    currentTicketName: String?,
    duplicateError: String?,
    suggestedName: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tableName by remember(currentTicketName, suggestedName) {
        mutableStateOf(suggestedName ?: currentTicketName ?: "")
    }
    LaunchedEffect(suggestedName) {
        if (suggestedName != null) tableName = suggestedName
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan ke Meja") },
        text = {
            Column {
                if (duplicateError != null) {
                    Text(
                        duplicateError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama Meja") },
                    placeholder = { Text("cth: Meja 1, VIP, dll") },
                    singleLine = true,
                    isError = duplicateError != null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (tableName.isNotBlank()) onSave(tableName.trim()) },
                enabled = tableName.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun TicketListDialog(
    tickets: Map<String, OpenTicket>,
    settings: com.lokalpos.app.util.SettingsManager,
    onLoad: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Tickets (${tickets.size})") },
        text = {
            if (tickets.isEmpty()) {
                Text("Belum ada meja yang tersimpan")
            } else {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tickets.entries.toList()) { (name, ticket) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onLoad(name) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${ticket.cart.sumOf { it.quantity }} item",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        settings.formatCurrency(ticket.cart.sumOf { it.subtotal }),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(onClick = { onDelete(name) }) {
                                        Icon(Icons.Filled.Close, "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
