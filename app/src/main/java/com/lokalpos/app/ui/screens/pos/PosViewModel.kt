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
    val discount: Double = 0.0,
    val notes: String = ""
) {
    val subtotal: Double get() = (product.price * quantity) - discount
}

data class PosUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val showCheckout: Boolean = false,
    val paymentMethod: String = "Tunai",
    val amountPaid: String = "",
    val isProcessing: Boolean = false,
    val completedTransaction: Transaction? = null,
    val completedItems: List<TransactionItem> = emptyList(),
    val showSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val subtotal: Double get() = cart.sumOf { it.subtotal }
    val discountTotal: Double get() {
        return if (discountPercent > 0) subtotal * discountPercent / 100.0 else discountAmount
    }
    val taxPercent: Double get() = 0.0
    val taxAmount: Double get() = 0.0
    val total: Double get() = subtotal - discountTotal + taxAmount
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
    private val customerRepo = app.customerRepository
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
        viewModelScope.launch {
            customerRepo.getAllCustomers().collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }

        _uiState.update {
            it.copy(
                discountPercent = 0.0,
                discountAmount = 0.0
            )
        }

        if (settings.taxEnabled) {
            // Tax is handled via settings
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

    fun addByBarcode(barcode: String) {
        viewModelScope.launch {
            val product = productRepo.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product)
            } else {
                _uiState.update { it.copy(errorMessage = "Produk dengan barcode '$barcode' tidak ditemukan") }
            }
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
        _uiState.update { it.copy(
            cart = emptyList(),
            selectedCustomer = null,
            discountPercent = 0.0,
            discountAmount = 0.0,
            showCheckout = false,
            amountPaid = "",
            paymentMethod = "Tunai"
        ) }
    }

    fun setDiscount(percent: Double = 0.0, amount: Double = 0.0) {
        _uiState.update { it.copy(discountPercent = percent, discountAmount = amount) }
    }

    fun selectCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setAmountPaid(amount: String) {
        _uiState.update { it.copy(amountPaid = amount) }
    }

    fun toggleCheckout() {
        _uiState.update { it.copy(showCheckout = !it.showCheckout) }
    }

    fun processPayment() {
        val state = _uiState.value
        if (!state.canPay) return

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            try {
                val receiptNumber = transactionRepo.generateReceiptNumber()

                val taxPct = if (settings.taxEnabled) settings.taxPercent else 0.0
                val taxAmt = if (taxPct > 0) state.subtotal * taxPct / 100.0 else 0.0
                val totalWithTax = state.subtotal - state.discountTotal + taxAmt

                val paidAmount = if (state.paymentMethod == "Tunai") {
                    state.amountPaid.toDoubleOrNull() ?: totalWithTax
                } else {
                    totalWithTax
                }

                val transaction = Transaction(
                    receiptNumber = receiptNumber,
                    customerId = state.selectedCustomer?.id,
                    customerName = state.selectedCustomer?.name,
                    subtotal = state.subtotal,
                    discountAmount = state.discountTotal,
                    discountPercent = state.discountPercent,
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
                        discount = cartItem.discount,
                        subtotal = cartItem.subtotal,
                        notes = cartItem.notes
                    )
                }

                val txId = transactionRepo.createTransaction(transaction, items)

                // Update stock
                for (cartItem in state.cart) {
                    if (cartItem.product.trackStock) {
                        productRepo.decreaseStock(cartItem.product.id, cartItem.quantity)
                    }
                }

                // Update customer loyalty
                state.selectedCustomer?.let { customer ->
                    val points = if (settings.loyaltyEnabled) {
                        (totalWithTax / settings.loyaltyPointsPerAmount).toInt()
                    } else 0
                    customerRepo.addPurchase(customer.id, totalWithTax, points)
                }

                val savedTx = transactionRepo.getTransactionById(txId) ?: transaction.copy(id = txId)
                val savedItems = transactionRepo.getTransactionItems(txId)

                _uiState.update { it.copy(
                    isProcessing = false,
                    completedTransaction = savedTx,
                    completedItems = savedItems,
                    showSuccess = true,
                    showCheckout = false
                ) }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isProcessing = false,
                    errorMessage = "Gagal memproses: ${e.message}"
                ) }
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
        _uiState.update { it.copy(
            showSuccess = false,
            completedTransaction = null,
            completedItems = emptyList(),
            cart = emptyList(),
            selectedCustomer = null,
            discountPercent = 0.0,
            discountAmount = 0.0,
            amountPaid = "",
            paymentMethod = "Tunai"
        ) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
