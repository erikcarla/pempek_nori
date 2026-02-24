package com.lokalpos.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToLong

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lokalpos_settings", Context.MODE_PRIVATE)

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

    var appPassword: String
        get() = prefs.getString("app_password", "IkanKakap46") ?: "IkanKakap46"
        set(value) = prefs.edit().putString("app_password", value).apply()

    var emailReportAddress: String
        get() = prefs.getString("email_report_address", "ribka.apriliana.09@gmail.com") ?: "ribka.apriliana.09@gmail.com"
        set(value) = prefs.edit().putString("email_report_address", value).apply()

    var productDisplayMode: String
        get() = prefs.getString("product_display_mode", "grid") ?: "grid"
        set(value) = prefs.edit().putString("product_display_mode", value).apply()

    var emailReportEnabled: Boolean
        get() = prefs.getBoolean("email_report_enabled", false)
        set(value) = prefs.edit().putBoolean("email_report_enabled", value).apply()

    var emailSenderAddress: String
        get() = prefs.getString("email_sender_address", "") ?: ""
        set(value) = prefs.edit().putString("email_sender_address", value).apply()

    var emailSenderPassword: String
        get() = prefs.getString("email_sender_password", "") ?: ""
        set(value) = prefs.edit().putString("email_sender_password", value).apply()

    var paymentMethods: Set<String>
        get() = prefs.getStringSet("payment_methods", setOf("Tunai", "QRIS BNI", "QRIS BCA", "BCA", "BNI", "Transfer BCA", "Transfer BNI")) ?: setOf("Tunai", "QRIS BNI", "QRIS BCA", "BCA", "BNI", "Transfer BCA", "Transfer BNI")
        set(value) = prefs.edit().putStringSet("payment_methods", value).apply()

    fun formatCurrency(amount: Double): String {
        val rounded = amount.roundToLong()
        val formatted = "%,d".format(rounded).replace(',', '.')
        return "$currencySymbol $formatted"
    }
}
