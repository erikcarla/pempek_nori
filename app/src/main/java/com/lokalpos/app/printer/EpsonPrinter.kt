package com.lokalpos.app.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import com.lokalpos.app.data.entity.Transaction
import com.lokalpos.app.data.entity.TransactionItem
import com.lokalpos.app.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

class EpsonPrinter(private val context: Context) {

    companion object {
        private const val TAG = "EpsonPrinter"
        private const val ACTION_USB_PERMISSION = "com.lokalpos.app.USB_PERMISSION"
        private const val EPSON_VENDOR_ID = 0x04B8

        // ESC/POS Commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40)
        private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        private val ESC_DOUBLE_WIDTH = byteArrayOf(0x1B, 0x21, 0x20)
        private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x10)
        private val ESC_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)
        private val ESC_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x01)
        private val ESC_FEED_LINES = byteArrayOf(0x1B, 0x64, 0x05)
        private val LF = byteArrayOf(0x0A)
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    fun findPrinter(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == EPSON_VENDOR_ID
        }
    }

    suspend fun requestPermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) return true

        return suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    context.unregisterReceiver(this)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (cont.isActive) cont.resume(granted)
                }
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE else 0

            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(ACTION_USB_PERMISSION), flags)

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }

            usbManager.requestPermission(device, permissionIntent)

            cont.invokeOnCancellation {
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }
    }

    private fun sendData(device: UsbDevice, data: ByteArray): Boolean {
        val connection = usbManager.openDevice(device) ?: run {
            Log.e(TAG, "Failed to open device")
            return false
        }

        try {
            val iface = device.getInterface(0)
            connection.claimInterface(iface, true)

            var outEndpoint: UsbEndpoint? = null
            for (i in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(i)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    ep.direction == UsbConstants.USB_DIR_OUT) {
                    outEndpoint = ep
                    break
                }
            }

            if (outEndpoint == null) {
                Log.e(TAG, "No output endpoint found")
                return false
            }

            val chunkSize = outEndpoint.maxPacketSize
            var offset = 0
            while (offset < data.size) {
                val length = minOf(chunkSize, data.size - offset)
                val chunk = data.copyOfRange(offset, offset + length)
                val transferred = connection.bulkTransfer(outEndpoint, chunk, chunk.size, 5000)
                if (transferred < 0) {
                    Log.e(TAG, "Bulk transfer failed at offset $offset")
                    return false
                }
                offset += length
            }

            connection.releaseInterface(iface)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Print error", e)
            return false
        } finally {
            connection.close()
        }
    }

    suspend fun printReceipt(
        transaction: Transaction,
        items: List<TransactionItem>,
        settings: SettingsManager
    ): Boolean = withContext(Dispatchers.IO) {
        val device = findPrinter() ?: run {
            Log.e(TAG, "Printer not found")
            return@withContext false
        }

        if (!requestPermission(device)) {
            Log.e(TAG, "Permission denied")
            return@withContext false
        }

        val width = settings.receiptWidth
        val receipt = buildReceipt(transaction, items, settings, width)
        sendData(device, receipt)
    }

    private fun buildReceipt(
        transaction: Transaction,
        items: List<TransactionItem>,
        settings: SettingsManager,
        width: Int
    ): ByteArray {
        val buffer = mutableListOf<Byte>()

        fun addBytes(bytes: ByteArray) = buffer.addAll(bytes.toList())
        fun addLine(text: String) {
            addBytes(text.toByteArray(Charsets.US_ASCII))
            addBytes(LF)
        }
        fun addSeparator() = addLine("=".repeat(width))
        fun addDash() = addLine("-".repeat(width))

        addBytes(ESC_INIT)

        // Header
        addBytes(ESC_ALIGN_CENTER)
        addBytes(ESC_BOLD_ON)
        // Store name with +2 size (double width and height)
        addBytes(ESC_DOUBLE_WIDTH)
        addBytes(ESC_DOUBLE_HEIGHT)
        addLine(settings.storeName)
        addBytes(ESC_NORMAL_SIZE)
        addBytes(ESC_BOLD_OFF)

        // Address and phone with normal size (no double)
        if (settings.storeAddress.isNotBlank()) {
            addLine(settings.storeAddress)
        }
        if (settings.storePhone.isNotBlank()) {
            addLine(settings.storePhone)
        }
        if (settings.receiptHeader.isNotBlank()) {
            settings.receiptHeader.split("\n").forEach { addLine(it) }
        }

        addSeparator()
        addBytes(ESC_ALIGN_LEFT)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        val dateStr = dateFormat.format(Date(transaction.createdAt))
        addLine("No: ${transaction.receiptNumber}")
        addLine("Tanggal: $dateStr")
        if (!transaction.customerName.isNullOrBlank()) {
            addLine("Pelanggan: ${transaction.customerName}")
        }

        addDash()

        // Items
        for (item in items) {
            val nameStr = item.productName
            val qtyPrice = "${item.quantity} x ${formatNum(item.unitPrice)}"
            val totalStr = formatNum(item.subtotal)

            addLine(nameStr)
            addLine(padLeftRight("  $qtyPrice", totalStr, width))

            if (item.discount > 0) {
                addLine(padLeftRight("  Diskon", "-${formatNum(item.discount)}", width))
            }
        }

        addDash()

        // Remove subtotal - go directly to discount if present
        if (transaction.discountAmount > 0) {
            val discLabel = if (transaction.discountPercent > 0)
                "Diskon (${transaction.discountPercent.toInt()}%)" else "Diskon"
            addLine(padLeftRight(discLabel, "-${formatNum(transaction.discountAmount)}", width))
        }

        addDash()
        addBytes(ESC_BOLD_ON)
        addLine(padLeftRight("TOTAL", formatNum(transaction.totalAmount), width))
        addBytes(ESC_BOLD_OFF)

        // Tax (PB1) appears AFTER total
        if (transaction.taxAmount > 0) {
            val taxLabel = if (transaction.taxPercent > 0)
                "PB1 (${transaction.taxPercent.toInt()}%)" else "PB1"
            addLine(padLeftRight(taxLabel, formatNum(transaction.taxAmount), width))
        }

        addLine(padLeftRight("Bayar (${transaction.paymentMethod})", formatNum(transaction.amountPaid), width))
        if (transaction.changeAmount > 0) {
            addLine(padLeftRight("Kembalian", formatNum(transaction.changeAmount), width))
        }

        addSeparator()

        // Footer
        addBytes(ESC_ALIGN_CENTER)
        val footer = settings.receiptFooter
        if (footer.isNotBlank()) {
            footer.split("\n").forEach { addLine(it) }
        }
        addSeparator()

        addBytes(ESC_FEED_LINES)
        addBytes(ESC_CUT_PAPER)

        return buffer.toByteArray()
    }

    private fun formatNum(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            "%,.0f".format(value)
        } else {
            "%,.2f".format(value)
        }
    }

    private fun padLeftRight(left: String, right: String, width: Int): String {
        val space = width - left.length - right.length
        return if (space > 0) {
            left + " ".repeat(space) + right
        } else {
            left + " " + right
        }
    }
}
