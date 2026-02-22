# LokalPOS - Aplikasi Kasir / POS Android

Aplikasi Point of Sale (POS) gratis untuk UMKM Indonesia. Semua data disimpan secara lokal di perangkat (tanpa cloud/server).

## Fitur Utama

### Kasir (POS)
- Penjualan dengan keranjang belanja
- Pencarian produk dan scan barcode
- Filter produk berdasarkan kategori
- Diskon per transaksi (persen atau nominal)
- Multiple metode pembayaran (Tunai, Kartu Debit, Kartu Kredit, QRIS, Transfer Bank)
- Perhitungan kembalian otomatis
- Quick amount buttons untuk pembayaran tunai

### Manajemen Produk
- CRUD produk lengkap (tambah, edit, hapus)
- Kategori produk dengan warna
- Barcode & SKU support
- Pelacakan stok (opsional per produk)
- Peringatan stok rendah otomatis
- Harga jual dan harga modal

### Riwayat Transaksi
- Lihat semua transaksi (hari ini, minggu, bulan, semua)
- Detail transaksi lengkap
- Cetak ulang struk
- Refund transaksi
- Auto-delete transaksi lama (default 30 hari, bisa diatur)

### Laporan Penjualan
- Total penjualan, jumlah transaksi, rata-rata
- Penjualan harian
- Ringkasan metode pembayaran
- Produk terlaris

### Manajemen Pelanggan (CRM)
- Database pelanggan
- Riwayat pembelian per pelanggan
- Program loyalti (poin)

### Cetak Struk
- Support printer **Epson TM-U220D** (dot matrix) via USB
- Header struk custom (nama toko, alamat, telepon, teks tambahan)
- Footer struk custom
- Lebar struk bisa diatur (42/56 karakter)

### Pengaturan
- Informasi toko
- Kustomisasi struk (header & footer)
- Setting printer
- Pajak (aktif/nonaktif, persentase)
- Auto-delete transaksi (berapa hari)
- Simbol mata uang
- Program loyalti

## Tech Stack

- **Bahasa**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database**: Room (SQLite) - Local Storage
- **Architecture**: MVVM
- **Background Tasks**: WorkManager
- **Printer**: ESC/POS via USB Host API
- **Min SDK**: 26 (Android 8.0)

## Build APK

### Via GitHub Actions (Recommended)
1. Push kode ke GitHub repository
2. GitHub Actions akan otomatis build APK
3. Download APK dari tab **Actions** > pilih workflow run > **Artifacts**

Untuk membuat release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Build Lokal
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

APK output ada di: `app/build/outputs/apk/`

## Setup Signing Key (Opsional, untuk Release)

Untuk sign APK release via GitHub Actions:

1. Generate keystore:
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lokalpos
```

2. Encode ke base64:
```bash
base64 -i release.jks | tr -d '\n'
```

3. Tambahkan GitHub Secrets:
   - `SIGNING_KEY`: Output base64 dari keystore
   - `KEY_ALIAS`: `lokalpos`
   - `KEY_STORE_PASSWORD`: Password keystore
   - `KEY_PASSWORD`: Password key

## Koneksi Printer

### Epson TM-U220D via USB
1. Hubungkan printer ke tablet/HP Android menggunakan kabel USB OTG
2. Buka **Pengaturan** > aktifkan **Printer**
3. Tekan **Test Koneksi Printer** untuk memastikan printer terdeteksi
4. Atur lebar struk (42 untuk Font A, 56 untuk Font B)

## Auto-Delete Transaksi

Secara default, transaksi yang lebih lama dari **30 hari** akan otomatis dihapus. Anda bisa mengubah setting ini di **Pengaturan** > **Manajemen Data** > **Hapus transaksi otomatis setelah (hari)**.

Set ke `0` untuk menonaktifkan auto-delete.

## Lisensi

Open source - bebas digunakan dan dimodifikasi.
