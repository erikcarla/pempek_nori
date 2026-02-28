package com.lokalpos.app.ui.screens.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lokalpos.app.LokalPosApp
import com.lokalpos.app.data.entity.*
import com.lokalpos.app.printer.EpsonPrinter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val notes: String = ""
) {
    val subtotal: Double get() = product.price * quantity
}

data class OpenTicket(
    val tableName: String,
    val cart: List<CartItem>,
    val createdAt: Long = System.currentTimeMillis()
)

data class SerializableCartItem(
    val productId: Long,
    val quantity: Int,
    val notes: String
)

data class PosUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val showCheckout: Boolean = false,
    val paymentMethod: String = "QRIS BNI",
    val amountPaid: String = "",
    val isProcessing: Boolean = false,
    val completedTransaction: Transaction? = null,
    val completedItems: List<TransactionItem> = emptyList(),
    val showSuccess: Boolean = false,
    val errorMessage: String? = null,
    val taxEnabled: Boolean = false,
    val taxPercent: Double = 0.0,
    val taxInclusive: Boolean = true,
    val openTickets: Map<String, OpenTicket> = emptyMap(),
    val currentTicketName: String? = null,
    val showTicketDialog: Boolean = false,
    val showTicketList: Boolean = false,
    val duplicateTicketWarning: String? = null,
    val suggestedTicketName: String? = null,
    val showQuantityDialog: Boolean = false,
    val editingCartItem: CartItem? = null
) {
    val subtotal: Double get() = cart.sumOf { it.subtotal }
    val taxAmount: Double get() {
        if (!taxEnabled || taxPercent <= 0) return 0.0
        return if (taxInclusive) {
            subtotal * taxPercent / (100 + taxPercent)
        } else {
            subtotal * taxPercent / 100.0
        }
    }
    val total: Double get() {
        return if (taxInclusive || !taxEnabled) {
            subtotal
        } else {
            subtotal + taxAmount
        }
    }
    val changeAmount: Double get() {
        val cleanAmount = amountPaid.replace(".", "").replace(",", "")
        val paid = cleanAmount.toDoubleOrNull() ?: 0.0
        return if (paid > total) paid - total else 0.0
    }
    val canPay: Boolean get() {
        if (cart.isEmpty()) return false
        if (paymentMethod == "Tunai") {
            val cleanAmount = amountPaid.replace(".", "").replace(",", "")
            val paid = cleanAmount.toDoubleOrNull() ?: 0.0
            return paid >= total
        }
        return true
    }
    val cartItemCount: Int get() = cart.sumOf { it.quantity }
}

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LokalPosApp
    private val productRepo = app.productRepository
    private val transactionRepo = app.transactionRepository
    val settings = app.settingsManager
    private val printer = EpsonPrinter(application)
    private val gson = com.google.gson.Gson()
    private var ticketsLoaded = false

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadPersistedTickets(products: List<Product>) {
        if (ticketsLoaded) return
        ticketsLoaded = true

        val savedTicketsJson = settings.loadTickets()
        if (savedTicketsJson.isEmpty()) return

        val tickets = mutableMapOf<String, OpenTicket>()
        savedTicketsJson.forEach { (name, json) ->
            try {
                val cartItems = gson.fromJson(json, Array<SerializableCartItem>::class.java)
                val cart = cartItems.mapNotNull { item ->
                    val product = products.find { it.id == item.productId }
                    product?.let { CartItem(it, item.quantity, item.notes) }
                }
                if (cart.isNotEmpty()) {
                    tickets[name] = OpenTicket(name, cart)
                }
            } catch (e: Exception) {
                android.util.Log.e("PosViewModel", "Error loading ticket: $name", e)
            }
        }
        if (tickets.isNotEmpty()) {
            _uiState.update { it.copy(openTickets = tickets) }
        }
    }

    private fun persistTickets() {
        val ticketsJson = mutableMapOf<String, String>()
        _uiState.value.openTickets.forEach { (name, ticket) ->
            val items = ticket.cart.map { SerializableCartItem(it.product.id, it.quantity, it.notes) }
            ticketsJson[name] = gson.toJson(items)
        }
        settings.saveTickets(ticketsJson)
    }

    private fun loadData() {
        viewModelScope.launch {
            productRepo.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            productRepo.getAllProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
                // Load persisted tickets after products are available
                loadPersistedTickets(products)
            }
        }
        _uiState.update { it.copy(paymentMethod = settings.defaultPaymentMethod) }
        refreshTaxSettings()
    }

    fun refreshTaxSettings() {
        _uiState.update {
            it.copy(
                taxEnabled = settings.taxEnabled,
                taxPercent = settings.taxPercent,
                taxInclusive = settings.taxInclusive
            )
        }
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            val flow = if (categoryId != null) {
                productRepo.getProductsByCategory(categoryId)
            } else {
                productRepo.getAllProducts()
            }
            flow.collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                productRepo.getAllProducts().collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            } else {
                productRepo.searchProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            }
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { state ->
            val existingIndex = state.cart.indexOfFirst { it.product.id == product.id }
            val newCart = if (existingIndex >= 0) {
                state.cart.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(quantity = existing.quantity + 1)
                }
            } else {
                state.cart + CartItem(product)
            }
            state.copy(cart = newCart)
        }
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        _uiState.update { state ->
            if (quantity <= 0) {
                state.copy(cart = state.cart.filter { it.product.id != productId })
            } else {
                val newCart = state.cart.map {
                    if (it.product.id == productId) it.copy(quantity = quantity) else it
                }
                state.copy(cart = newCart)
            }
        }
    }

    fun removeFromCart(productId: Long) {
        _uiState.update { state ->
            state.copy(cart = state.cart.filter { it.product.id != productId })
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cart = emptyList(),
                showCheckout = false,
                amountPaid = "",
                paymentMethod = "Tunai",
                currentTicketName = null
            )
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { state ->
            val newState = state.copy(paymentMethod = method)
            if (method == "Tunai") {
                newState.copy(amountPaid = "%,.0f".format(state.total).replace(",", ""))
            } else {
                newState.copy(amountPaid = "")
            }
        }
    }

    fun setAmountPaid(amount: String) {
        // Allow only digits and optional dots for thousands separator
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(amountPaid = filtered) }
    }

    fun toggleCheckout() {
        _uiState.update { state ->
            val entering = !state.showCheckout
            if (entering) {
                val paymentMethods = settings.getPaymentMethodsList()
                val defaultMethod = paymentMethods.firstOrNull() ?: "QRIS BNI"
                val formatted = "%,d".format(state.total.toLong()).replace(",", ".")
                state.copy(
                    showCheckout = true,
                    paymentMethod = defaultMethod,
                    amountPaid = if (defaultMethod == "Tunai") formatted else ""
                )
            } else {
                state.copy(showCheckout = !state.showCheckout)
            }
        }
    }

    fun processPayment() {
        val state = _uiState.value
        if (!state.canPay) return

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            try {
                val receiptNumber = transactionRepo.generateReceiptNumber()

                val taxPct = if (settings.taxEnabled) settings.taxPercent else 0.0
                val taxAmt = if (taxPct > 0) {
                    if (settings.taxInclusive) {
                        state.subtotal * taxPct / (100 + taxPct)
                    } else {
                        state.subtotal * taxPct / 100.0
                    }
                } else 0.0
                val totalWithTax = if (settings.taxInclusive) state.subtotal else state.subtotal + taxAmt

                val paidAmount = if (state.paymentMethod == "Tunai") {
                    val cleanAmount = state.amountPaid.replace(".", "").replace(",", "")
                    cleanAmount.toDoubleOrNull() ?: totalWithTax
                } else {
                    totalWithTax
                }

                val transaction = Transaction(
                    receiptNumber = receiptNumber,
                    subtotal = state.subtotal,
                    discountAmount = 0.0,
                    discountPercent = 0.0,
                    taxAmount = taxAmt,
                    taxPercent = taxPct,
                    totalAmount = totalWithTax,
                    paymentMethod = state.paymentMethod,
                    amountPaid = paidAmount,
                    changeAmount = if (paidAmount > totalWithTax) paidAmount - totalWithTax else 0.0
                )

                val items = state.cart.map { cartItem ->
                    TransactionItem(
                        transactionId = 0,
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.product.price,
                        discount = 0.0,
                        subtotal = cartItem.subtotal,
                        notes = cartItem.notes
                    )
                }

                val txId = transactionRepo.createTransaction(transaction, items)

                for (cartItem in state.cart) {
                    if (cartItem.product.trackStock) {
                        productRepo.decreaseStock(cartItem.product.id, cartItem.quantity)
                    }
                }

                val savedTx = transactionRepo.getTransactionById(txId) ?: transaction.copy(id = txId)
                val savedItems = transactionRepo.getTransactionItems(txId)

                val ticketName = state.currentTicketName
                val updatedTickets = if (ticketName != null) {
                    state.openTickets - ticketName
                } else {
                    state.openTickets
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        completedTransaction = savedTx,
                        completedItems = savedItems,
                        showSuccess = true,
                        showCheckout = false,
                        openTickets = updatedTickets
                    )
                }
                persistTickets()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Gagal memproses: ${e.message}"
                    )
                }
            }
        }
    }

    fun printLastReceipt() {
        val state = _uiState.value
        val tx = state.completedTransaction ?: return
        val items = state.completedItems

        viewModelScope.launch {
            val success = printer.printReceipt(tx, items, settings)
            if (!success) {
                _uiState.update { it.copy(errorMessage = "Gagal mencetak struk. Periksa koneksi printer.") }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update {
            it.copy(
                showSuccess = false,
                completedTransaction = null,
                completedItems = emptyList(),
                cart = emptyList(),
                amountPaid = "",
                paymentMethod = "Tunai",
                currentTicketName = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Ticket / Table Management ---

    fun showTicketDialog() {
        _uiState.update { it.copy(showTicketDialog = true) }
    }

    fun hideTicketDialog() {
        _uiState.update { it.copy(showTicketDialog = false) }
    }

    fun showTicketList() {
        _uiState.update { it.copy(showTicketList = true) }
    }

    fun hideTicketList() {
        _uiState.update { it.copy(showTicketList = false) }
    }

    fun saveTicket(tableName: String) {
        val state = _uiState.value
        if (state.cart.isEmpty() || tableName.isBlank()) return

        // Check for duplicate name (but allow overwriting if it's the current ticket being edited)
        if (state.openTickets.containsKey(tableName) && state.currentTicketName != tableName) {
            // Generate suggested name
            var counter = 2
            var suggestedName = "$tableName($counter)"
            while (state.openTickets.containsKey(suggestedName)) {
                counter++
                suggestedName = "$tableName($counter)"
            }
            _uiState.update {
                it.copy(
                    duplicateTicketWarning = "Sudah ada meja \"$tableName\"!",
                    suggestedTicketName = suggestedName
                )
            }
            return
        }

        val ticket = OpenTicket(
            tableName = tableName,
            cart = state.cart
        )
        _uiState.update {
            it.copy(
                openTickets = it.openTickets + (tableName to ticket),
                cart = emptyList(),
                currentTicketName = null,
                showTicketDialog = false,
                showCheckout = false,
                amountPaid = "",
                duplicateTicketWarning = null,
                suggestedTicketName = null
            )
        }
        persistTickets()
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(duplicateTicketWarning = null, suggestedTicketName = null) }
    }

    fun useSuggestedTicketName() {
        val suggested = _uiState.value.suggestedTicketName ?: return
        _uiState.update { it.copy(duplicateTicketWarning = null, suggestedTicketName = null) }
        saveTicket(suggested)
    }

    fun loadTicket(tableName: String) {
        val state = _uiState.value
        val ticket = state.openTickets[tableName] ?: return

        var updatedTickets = state.openTickets - tableName

        if (state.cart.isNotEmpty() && state.currentTicketName != null) {
            val currentTicket = OpenTicket(
                tableName = state.currentTicketName,
                cart = state.cart
            )
            updatedTickets = updatedTickets + (state.currentTicketName to currentTicket)
        }

        _uiState.update {
            it.copy(
                openTickets = updatedTickets,
                cart = ticket.cart,
                currentTicketName = tableName,
                showTicketList = false,
                showCheckout = false,
                amountPaid = ""
            )
        }
        persistTickets()
    }

    fun deleteTicket(tableName: String) {
        _uiState.update {
            it.copy(openTickets = it.openTickets - tableName)
        }
        persistTickets()
    }

    // Quantity dialog
    fun showQuantityDialog(cartItem: CartItem) {
        _uiState.update { it.copy(showQuantityDialog = true, editingCartItem = cartItem) }
    }

    fun hideQuantityDialog() {
        _uiState.update { it.copy(showQuantityDialog = false, editingCartItem = null) }
    }

    fun updateQuantityAndClose(productId: Long, quantity: Int) {
        updateCartQuantity(productId, quantity)
        hideQuantityDialog()
    }
}
