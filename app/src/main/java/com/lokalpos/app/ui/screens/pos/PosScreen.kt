package com.lokalpos.app.ui.screens.pos

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lokalpos.app.ui.theme.SuccessGreen
import kotlin.math.roundToInt

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
            onSave = { viewModel.saveTicket(it) },
            onDismiss = { viewModel.hideTicketDialog() }
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

    // Duplicate ticket warning dialog
    state.duplicateTicketWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateWarning() },
            title = { Text("Nama Meja Duplikat") },
            text = {
                Column {
                    Text(warning)
                    state.suggestedTicketName?.let { suggested ->
                        Spacer(Modifier.height(8.dp))
                        Text("Gunakan nama \"$suggested\"?")
                    }
                }
            },
            confirmButton = {
                state.suggestedTicketName?.let {
                    Button(onClick = { viewModel.useSuggestedTicketName() }) {
                        Text("Gunakan")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateWarning() }) {
                    Text("Batal")
                }
            }
        )
    }

    // Quantity edit dialog
    if (state.showQuantityDialog && state.editingCartItem != null) {
        QuantityEditDialog(
            cartItem = state.editingCartItem!!,
            settings = settings,
            onUpdateQuantity = { productId, qty -> viewModel.updateQuantityAndClose(productId, qty) },
            onDelete = { productId ->
                viewModel.removeFromCart(productId)
                viewModel.hideQuantityDialog()
            },
            onDismiss = { viewModel.hideQuantityDialog() }
        )
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // For tablet: use Row layout with separate headers
    if (isTablet) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Left side: Product area (70%)
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
            ) {
                // Product header (green)
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.searchProducts(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Cari produk...",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (state.currentTicketName != null) state.currentTicketName!! else "Kasir",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                // Category dropdown
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { categoryDropdownExpanded = true },
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                state.categories.find { it.id == state.selectedCategoryId }?.name ?: "All items",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                if (categoryDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                                null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false },
                                        modifier = Modifier.widthIn(min = 180.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All items") },
                                            onClick = {
                                                viewModel.selectCategory(null)
                                                categoryDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                if (state.selectedCategoryId == null) {
                                                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                        state.categories.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(category.name) },
                                                onClick = {
                                                    viewModel.selectCategory(category.id)
                                                    categoryDropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (state.selectedCategoryId == category.id) {
                                                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.searchProducts("")
                            }) {
                                Icon(Icons.Filled.ArrowBack, "Tutup")
                            }
                        } else {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Filled.Menu, "Menu")
                            }
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchProducts("") }) {
                                    Icon(Icons.Filled.Close, "Hapus")
                                }
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Filled.Search, "Cari")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                // Products
                ProductPanel(state = state, settings = settings, viewModel = viewModel)
            }

            VerticalDivider()

            // Right side: Cart area (30%)
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                // Cart header (white/surface color like "Ticket" in screenshot)
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (state.currentTicketName != null) state.currentTicketName!! else "Ticket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        // Open tickets button with badge
                        BadgedBox(
                            badge = {
                                if (state.openTickets.isNotEmpty()) {
                                    Badge { Text("${state.openTickets.size}") }
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.showTicketList() }) {
                                Icon(Icons.Filled.TableBar, "Open Tickets")
                            }
                        }
                    }
                }
                // Cart content
                if (state.showCheckout) {
                    TabletCheckoutPanel(state = state, settings = settings, viewModel = viewModel)
                } else {
                    TabletCartContent(state = state, settings = settings, viewModel = viewModel)
                }
            }
        }
    } else {
        // Phone layout
        // If checkout is showing, display PhoneCheckoutView directly (no Kasir header)
        if (state.showCheckout) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                PhoneCheckoutView(state = state, settings = settings, viewModel = viewModel)
            }
        } else {
            // Normal phone layout with Kasir header
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.searchProducts(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Cari produk...",
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (state.currentTicketName != null) state.currentTicketName!! else "Kasir",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            // Category dropdown
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { categoryDropdownExpanded = true },
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            state.categories.find { it.id == state.selectedCategoryId }?.name ?: "All items",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            if (categoryDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                            null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    modifier = Modifier.widthIn(min = 180.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All items") },
                                        onClick = {
                                            viewModel.selectCategory(null)
                                            categoryDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            if (state.selectedCategoryId == null) {
                                                Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                            }
                                        }
                                    )
                                    state.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                viewModel.selectCategory(category.id)
                                                categoryDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                if (state.selectedCategoryId == category.id) {
                                                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.searchProducts("")
                        }) {
                            Icon(Icons.Filled.ArrowBack, "Tutup")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, "Menu")
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchProducts("") }) {
                                Icon(Icons.Filled.Close, "Hapus")
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, "Cari")
                        }
                        BadgedBox(
                            badge = {
                                if (state.openTickets.isNotEmpty()) {
                                    Badge { Text("${state.openTickets.size}") }
                                }
                            }
                        ) {
                            IconButton(onClick = { viewModel.showTicketList() }) {
                                Icon(Icons.Filled.TableBar, "Open Tickets")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            PhoneProductsView(
                state = state,
                settings = settings,
                viewModel = viewModel
            )
            }
        }
    }
}

// ========================= TABLET CART CONTENT =========================

@Composable
private fun TabletCartContent(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                    // Delete button
                    OutlinedButton(
                        onClick = { viewModel.clearCart() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, "Hapus", Modifier.size(20.dp))
                    }
                    OutlinedButton(
                        onClick = { viewModel.showTicketDialog() },
                        modifier = Modifier.weight(1f)
                    ) { Text("SAVE") }
                    Button(
                        onClick = { viewModel.toggleCheckout() },
                        modifier = Modifier.weight(1f)
                    ) { Text("BAYAR") }
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.getPaymentMethodsList()) { method ->
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
private fun PhoneProductsView(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    var showCartExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Product panel takes remaining space
        Column(modifier = Modifier.weight(1f)) {
            ProductPanel(state = state, settings = settings, viewModel = viewModel)
        }

        // Cart section
        if (state.cart.isNotEmpty()) {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Column {
                    // Cart header - clickable to expand/collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCartExpanded = !showCartExpanded }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showCartExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                contentDescription = if (showCartExpanded) "Tutup" else "Buka",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${state.cartItemCount} item", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            settings.formatCurrency(state.total),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Expandable cart items list
                    if (showCartExpanded) {
                        HorizontalDivider()
                        LazyColumn(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(state.cart) { cartItem ->
                                    CartItemRow(cartItem, settings, viewModel)
                                }
                            }
                            HorizontalDivider()
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                Text("Save", fontSize = 12.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneCheckoutView(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pembayaran") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleCheckout() }) {
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
                .imePadding()
        ) {
            // Cart items - scrollable with edit/delete capability
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.cart) { cartItem ->
                    CartItemRow(cartItem, settings, viewModel)
                }
            }

            // Payment section - fixed at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (state.taxAmount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PB1 (${state.taxPercent.toInt()}%${if (state.taxInclusive) ", inc" else ""})")
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

                Spacer(Modifier.height(8.dp))

                // Payment methods - no label
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(settings.getPaymentMethodsList()) { method ->
                        FilterChip(
                            selected = state.paymentMethod == method,
                            onClick = { viewModel.setPaymentMethod(method) },
                            label = { Text(method) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (state.paymentMethod == "Tunai") {
                    CashPaymentInput(state, settings, viewModel)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = { viewModel.processPayment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = state.canPay && !state.isProcessing,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("BAYAR ${settings.formatCurrency(state.total)}", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// Simple cart item row for checkout view (non-swipeable)
@Composable
private fun CartItemRowSimple(
    cartItem: CartItem,
    settings: com.lokalpos.app.util.SettingsManager
) {
    Card(
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                fontSize = 13.sp
            )
        }
    }
}

// ========================= SHARED COMPONENTS =========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPanel(
    state: PosUiState,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Product display - Grid or List based on settings
        val isGridMode = settings.displayMode == "grid"

        // Sort products: symbols/numbers first, then alphabetical A-Z
        val sortedProducts = remember(state.products) {
            state.products.sortedWith(compareBy { product ->
                val firstChar = product.name.firstOrNull() ?: 'z'
                when {
                    !firstChar.isLetter() -> "0${product.name.lowercase()}"
                    else -> "1${product.name.lowercase()}"
                }
            })
        }

        if (isGridMode) {
            // Product grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedProducts, key = { it.id }) { product ->
                    ProductGridItem(
                        product = product,
                        currencySymbol = settings.currencySymbol,
                        onClick = { viewModel.addToCart(product) }
                    )
                }
            }
        } else {
            // Product list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sortedProducts, key = { it.id }) { product ->
                    ProductListItem(
                        product = product,
                        settings = settings,
                        onClick = { viewModel.addToCart(product) }
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
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$currencySymbol%,d".format(product.price.toLong()),
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
private fun ProductListItem(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.trackStock && product.inStock <= product.lowStockAlert) {
                    Text(
                        text = "Stok: ${product.inStock}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = settings.formatCurrency(product.price),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    settings: com.lokalpos.app.util.SettingsManager,
    viewModel: PosViewModel
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isSwiped by remember { mutableStateOf(false) }
    val swipeThreshold = 40f
    val swipeOffset = 70f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Red background with delete icon - always present behind the card
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.error)
                .clickable {
                    viewModel.removeFromCart(cartItem.entryId)
                    isSwiped = false
                    offsetX = 0f
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Hapus",
                tint = Color.White,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(24.dp)
            )
        }

        // White card that slides to reveal red background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -swipeThreshold) {
                                offsetX = -swipeOffset
                                isSwiped = true
                            } else {
                                offsetX = 0f
                                isSwiped = false
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-swipeOffset - 20f, 0f)
                        }
                    )
                }
                .clickable {
                    if (isSwiped) {
                        offsetX = 0f
                        isSwiped = false
                    } else {
                        viewModel.showQuantityDialog(cartItem)
                    }
                },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    fontSize = 13.sp
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
            OutlinedButton(
                onClick = { viewModel.setAmountPaid("%,.0f".format(amount).replace(",", "")) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("%,.0f".format(amount), fontSize = 11.sp)
            }
        }
    }
}

// ========================= DIALOGS =========================

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
        title = {
            Text(
                "Pembayaran Berhasil!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onPrint) {
                        Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cetak Struk")
                    }
                    Button(onClick = onDismiss) {
                        Text("Selesai")
                    }
                }
            }
        }
    )
}

@Composable
private fun SaveTicketDialog(
    currentTicketName: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tableName by remember { mutableStateOf(currentTicketName ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan ke Meja") },
        text = {
            OutlinedTextField(
                value = tableName,
                onValueChange = { tableName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Meja") },
                placeholder = { Text("cth: Meja 1, VIP, dll") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (tableName.isNotBlank()) onSave(tableName.trim()) }, enabled = tableName.isNotBlank()) {
                Text("Save")
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

@Composable
private fun QuantityEditDialog(
    cartItem: CartItem,
    settings: com.lokalpos.app.util.SettingsManager,
    onUpdateQuantity: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(cartItem.quantity) }
    var manualInput by remember { mutableStateOf(cartItem.quantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                cartItem.product.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Price info
                Text(
                    "${quantity} x ${settings.formatCurrency(cartItem.product.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    settings.formatCurrency(cartItem.product.price * quantity),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                // Quantity controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (quantity > 1) {
                                quantity--
                                manualInput = quantity.toString()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Remove, "Kurang")
                    }

                    Spacer(Modifier.width(16.dp))

                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { value ->
                            manualInput = value.filter { it.isDigit() }
                            manualInput.toIntOrNull()?.let {
                                if (it > 0) quantity = it
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(Modifier.width(16.dp))

                    FilledIconButton(
                        onClick = {
                            quantity++
                            manualInput = quantity.toString()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Tambah")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Delete button
                OutlinedButton(
                    onClick = { onDelete(cartItem.entryId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Hapus Item")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateQuantity(cartItem.entryId, quantity) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
