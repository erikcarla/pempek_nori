package com.lokalpos.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.roundToLong

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lokalpos_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

    // Password protection (default: IkanKakap46)
    var appPassword: String
        get() = prefs.getString("app_password", "IkanKakap46") ?: "IkanKakap46"
        set(value) = prefs.edit().putString("app_password", value).apply()

    fun validatePassword(password: String): Boolean {
        return password == appPassword
    }

    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (oldPassword != appPassword) return false
        if (newPassword.isBlank()) return false
        appPassword = newPassword
        return true
    }

    // Display mode: "grid" or "list"
    var displayMode: String
        get() = prefs.getString("display_mode", "grid") ?: "grid"
        set(value) = prefs.edit().putString("display_mode", value).apply()

    var storeName: String
        get() = prefs.getString("store_name", "Pempek Nori") ?: "Pempek Nori"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var storeAddress: String
        get() = prefs.getString("store_address", "") ?: ""
        set(value) = prefs.edit().putString("store_address", value).apply()

    var storePhone: String
        get() = prefs.getString("store_phone", "") ?: ""
        set(value) = prefs.edit().putString("store_phone", value).apply()

    var receiptHeader: String
        get() = prefs.getString("receipt_header", "") ?: ""
        set(value) = prefs.edit().putString("receipt_header", value).apply()

    var receiptFooter: String
        get() = prefs.getString("receipt_footer", "Terima Kasih!\nSelamat Datang Kembali") ?: "Terima Kasih!\nSelamat Datang Kembali"
        set(value) = prefs.edit().putString("receipt_footer", value).apply()

    var taxEnabled: Boolean
        get() = prefs.getBoolean("tax_enabled", false)
        set(value) = prefs.edit().putBoolean("tax_enabled", value).apply()

    var taxPercent: Double
        get() = prefs.getFloat("tax_percent", 10f).toDouble()
        set(value) = prefs.edit().putFloat("tax_percent", value.toFloat()).apply()

    var taxInclusive: Boolean
        get() = prefs.getBoolean("tax_inclusive", true)
        set(value) = prefs.edit().putBoolean("tax_inclusive", value).apply()

    var autoDeleteDays: Int
        get() = prefs.getInt("auto_delete_days", 30)
        set(value) = prefs.edit().putInt("auto_delete_days", value).apply()

    var currencySymbol: String
        get() = prefs.getString("currency_symbol", "Rp") ?: "Rp"
        set(value) = prefs.edit().putString("currency_symbol", value).apply()

    var printerConnectionType: String
        get() = prefs.getString("printer_connection", "USB") ?: "USB"
        set(value) = prefs.edit().putString("printer_connection", value).apply()

    var printerEnabled: Boolean
        get() = prefs.getBoolean("printer_enabled", false)
        set(value) = prefs.edit().putBoolean("printer_enabled", value).apply()

    var receiptWidth: Int
        get() = prefs.getInt("receipt_width", 33)
        set(value) = prefs.edit().putInt("receipt_width", value).apply()

    var emailReportEnabled: Boolean
        get() = prefs.getBoolean("email_report_enabled", false)
        set(value) = prefs.edit().putBoolean("email_report_enabled", value).apply()

    var emailReportAddress: String
        get() = prefs.getString("email_report_address", "ribka.apriliana.09@gmail.com") ?: "ribka.apriliana.09@gmail.com"
        set(value) = prefs.edit().putString("email_report_address", value).apply()

    var emailSenderAddress: String
        get() = prefs.getString("email_sender_address", "") ?: ""
        set(value) = prefs.edit().putString("email_sender_address", value).apply()

    var emailSenderPassword: String
        get() = prefs.getString("email_sender_password", "") ?: ""
        set(value) = prefs.edit().putString("email_sender_password", value).apply()

    fun getPaymentMethodsList(): List<String> {
        // Fixed order: QRIS BNI, BCA, Tunai, BNI, QRIS BCA, Transfer BCA, Transfer BNI
        val defaultOrder = listOf("QRIS BNI", "BCA", "Tunai", "BNI", "QRIS BCA", "Transfer BCA", "Transfer BNI")
        val saved = prefs.getStringSet("payment_methods", defaultOrder.toSet()) ?: defaultOrder.toSet()
        return defaultOrder.filter { it in saved }
    }

    var paymentMethods: Set<String>
        get() = getPaymentMethodsList().toSet()
        set(value) = prefs.edit().putStringSet("payment_methods", value).apply()

    // Default payment method
    var defaultPaymentMethod: String
        get() = prefs.getString("default_payment_method", "QRIS BNI") ?: "QRIS BNI"
        set(value) = prefs.edit().putString("default_payment_method", value).apply()

    // Persisted tickets (JSON)
    fun saveTickets(tickets: Map<String, String>) {
        val json = gson.toJson(tickets)
        prefs.edit().putString("saved_tickets", json).apply()
    }

    fun loadTickets(): Map<String, String> {
        val json = prefs.getString("saved_tickets", null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun formatCurrency(amount: Double): String {
        val rounded = amount.roundToLong()
        val formatted = "%,d".format(rounded).replace(',', '.')
        return "$currencySymbol $formatted"
    }
}
