# ArAr TOZ 💰✨

**ArAr TOZ** adalah aplikasi pencatatan keuangan dan manajemen anggaran belanja rumah tangga cerdas berbasis **Offline-First Android** (Jetpack Compose, Room Database, Material 3). Dirancang khusus untuk membantu keluarga mengelola kas harian, membatasi pos pengeluaran dengan sistem amplop, mencatat target impian, serta menyediakan asisten cerdas lokal tanpa ketergantungan internet.

---

## 🌟 Fitur Utama

- ⚡ **Kalkulator Ekspresi Cepat & Akurat**: Input nominal fleksibel mendukung operasi matematika langsung (`50000+25000`, `15000*2`), pemisah ribuan titik/koma, dan nilai desimal tanpa pembulatan keliru.
- ✉️ **Sistem Pos Amplop Anggaran (Budget Envelopes)**: Batasi pengeluaran per kategori setiap bulan. Menghitung persentase terpakai secara real-time dan memberikan peringatan jika anggaran menipis atau melebihi kuota (*Overbudget*).
- 👛 **Multi-Sumber Dompet (Wallets)**: Pantau saldo di **Dompet Tunai**, **Rekening Bank**, dan **e-Wallet** secara terpisah dan sinkron.
- 📊 **Analitik Keuangan & Deteksi Defisit**: Visualisasi diagram donat kategori belanja, ringkasan pemasukan vs pengeluaran, serta deteksi dini peringatan defisit kas keluarga.
- 🎯 **Wishlist & Target Tabungan (Saving Goals)**: Kelola daftar impian belanja berdasarkan skala prioritas (*Tinggi*, *Sedang*, *Rendah*) dan progres tabungan.
- 🤖 **Offline Smart Assistant**: Input transaksi, pencatatan otomatis, dan tanya rekap saldo cukup dengan perintah teks alami berbahasa Indonesia yang diproses 100% lokal di perangkat (*Privacy First*).
- 📋 **Rekap Otomatis ke WhatsApp**: Bagikan ringkasan laporan kas dan belanja keluarga langsung ke grup WhatsApp keluarga dengan satu kali klik.
- 🔒 **Backup & Restore Lokal**: Cadangkan dan pulihkan seluruh riwayat data transaksi dan pengaturan dengan aman ke memori perangkat.

---

## 📱 Spesifikasi Aplikasi

| Parameter | Keterangan |
| :--- | :--- |
| **Nama Aplikasi** | **ArAr TOZ** |
| **Branding** | Catatan Kas & Belanja Keluarga |
| **Package ID (`applicationId`)** | `com.aistudio.arartoz.jvsd` |
| **Namespace** | `com.example` |
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 36 (Android 16) |
| **UI Framework** | Jetpack Compose & Material Design 3 |
| **Database** | Room SQLite Database (Offline-First) |
| **Arsitektur** | MVVM + StateFlow / Coroutines |

---

## 🚀 Panduan Membuka & Menjalankan Proyek

### Prasyarat
- **Android Studio** (Ladybug / Koala atau versi yang lebih baru)
- **JDK 17 atau JDK 21**
- **Android SDK API 36**

### Langkah Menjalankan:
1. Buka **Android Studio**.
2. Pilih **File** > **Open** lalu pilih direktori proyek ini (`ArAr TOZ`).
3. Tunggu proses *Gradle Sync* selesai mengunduh dependensi.
4. Hubungkan perangkat fisik via USB Debugging atau jalankan Emulator Android (API 24+).
5. Klik tombol **Run 'app'** (`Shift + F10`) untuk mengompilasi dan menginstal aplikasi ke perangkat.

---

## 📄 Lisensi
Hak Cipta © 2026 **GudangJoker / ArAr TOZ**. Seluruh hak cipta dilindungi undang-undang.
