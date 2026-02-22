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

data class PosUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val showCheckout: Boolean = false,
    val paymentMethod: String = "Tunai",
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
    val showTicketList: Boolean = false
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
        val paid = amountPaid.toDoubleOrNull() ?: 0.0
        return if (paid > total) paid - total else 0.0
    }
    val canPay: Boolean get() {
        if (cart.isEmpty()) return false
        if (paymentMethod == "Tunai") {
            val paid = amountPaid.toDoubleOrNull() ?: 0.0
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

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
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
            }
        }
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
        _uiState.update { it.copy(amountPaid = amount) }
    }

    fun toggleCheckout() {
        _uiState.update { state ->
            val entering = !state.showCheckout
            if (entering && state.paymentMethod == "Tunai") {
                state.copy(
                    showCheckout = true,
                    amountPaid = "%,.0f".format(state.total).replace(",", "")
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
                    state.amountPaid.toDoubleOrNull() ?: totalWithTax
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
                amountPaid = ""
            )
        }
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
    }

    fun deleteTicket(tableName: String) {
        _uiState.update {
            it.copy(openTickets = it.openTickets - tableName)
        }
    }
}
