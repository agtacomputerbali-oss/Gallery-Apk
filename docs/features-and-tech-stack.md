# 📚 Dokumentasi Fitur & Tech Stack — Gallery App Android

Dokumen ini berisi informasi komprehensif mengenai daftar modul dan fitur yang telah dikembangkan dalam aplikasi **Gallery App Android**, serta rincian *technology stack* (tech stack) yang digunakan untuk membangun proyek ini.

> **Versi Dokumen:** v1.1.0 (versionCode 6) — 28 Juli 2026 | Android Enterprise Application

---

## 🛠️ Technology Stack

Aplikasi ini dibangun menggunakan arsitektur modern Android berbasis **Kotlin** dan **Jetpack Compose** yang berfokus pada kecepatan pemrosesan berkas media, penggunaan memori yang efisien, dan keamanan data tingkat tinggi. Berikut adalah komponen *tech stack* yang digunakan:

### 1. Core Framework & Bahasa
* **Kotlin (v1.9+)**: Bahasa pemrograman utama yang menawarkan sintaks modern, keamanan pengetikan statis (*null-safety*), dan integrasi Coroutines yang efisien.
* **Jetpack Compose**: Framework UI deklaratif modern buatan Google untuk membangun antarmuka pengguna Android yang cepat, responsif, dan fleksibel tanpa menggunakan XML layouts.
* **Lifecycle Runtime Compose (`androidx.lifecycle:lifecycle-runtime-compose`)**: Pengambilan data `StateFlow` secara *lifecycle-aware* (`collectAsStateWithLifecycle`) untuk menghentikan rekomposisi otomatis saat aplikasi berada di background.
* **Material Design 3 (Material You)**: Komponen UI dasar yang aksesibel, modern, dan mendukung tema adaptif dinamis (*Dynamic Color*).

### 2. Paginasi & Pengelolaan Media
* **Jetpack Paging 3 (`androidx.paging:paging-compose`)**: Mengelola kueri berkas foto dari `MediaStore` secara bertahap (*chunking* 30 item per halaman, `initialLoadSize = 60`, `prefetchDistance = 15`, `maxSize = 200`) untuk mencegah masalah kehabisan memori (*Out Of Memory / OOM*) pada galeri berkapasitas ribuan foto.
* **Coil 3 (`coil-compose`)**: Pustaka pemuat gambar (*image loader*) berbasis Kotlin Coroutines. Dikonfigurasi secara terpusat pada `GalleryApplication` dengan *MemoryCache* (30% heap) dan *DiskCache* (5% disk) terukur, serta *ImageLoader* terisolasi tanpa cache khusus untuk Vault.

### 3. Arsitektur & Dependency Injection
* **MVVM (Model-View-ViewModel)**: Pola arsitektur resmi Android untuk memisahkan logika bisnis dari lapisan UI.
* **Hilt (Jetpack Dependency Injection)**: Framework injeksi dependensi terpusat (`AppModule`, `SettingsModule`) untuk mengelola siklus hidup ViewModel, Repository, dan Service.
* **Coroutines & Flow**: Pemrograman asinkron berbasis aliran data (*reactive stream*) untuk mengeksekusi kueri `ContentResolver` pada thread `Dispatchers.IO` tanpa mengganggu kelancaran UI (60fps/120fps UI thread).

### 4. Keamanan & Storage
* **Scoped Storage API**: Kepatuhan penuh terhadap aturan akses media Android 10+ (API 29+), menggunakan `MediaStore.createDeleteRequest()` dan `MediaStore.createTrashRequest()`.
* **BiometricPrompt (`androidx.biometric:biometric`)**: Otentikasi sidik jari atau wajah berbasis enkripsi hardware untuk membuka bilik rahasia (*Hidden Vault*).
* **Encrypted DataStore (`androidx.security:security-crypto`)**: Penyimpanan preferensi terenkripsi hardware-level untuk menyimpan PIN cadangan vault secara aman.

### 5. Build System & Perkakas Development
* **Gradle KTS (Kotlin Script)**: Sistem skrip build terstruktur berbasis Kotlin.
* **Version Catalog (`libs.versions.toml`)**: Manajemen terpusat untuk versi pustaka dan plugin guna mencegah bentrok dependensi.

---

## 📦 Alur & Modul Aplikasi (MVP Scope P0)

Seluruh modul dikembangkan berdasarkan alur penggunaan galeri Android yang efisien dan aman:

### 🛠️ Modul 1: Scaffold, DI & Izin Akses Runtime (Permissions) `[✅ TERIMPLEMENTASI]`
- **Multi-Version Runtime Permissions**: Mendukung pemanggilan izin `READ_MEDIA_IMAGES` untuk Android 13+ (API 33+) serta fallback `READ_EXTERNAL_STORAGE` untuk versi Android 8.0 - 12 (API 26–32).
- **Graceful Permission UI**: Tampilan penanganan saat izin ditolak (*Denied State*) di `PermissionScreen.kt` yang memberikan penjelasan rasional beserta pintasan langsung ke halaman Pengaturan Perangkat (*Settings*).

### 📸 Modul 2: Grid Thumbnail & Fast Scroll (Paging 3) `[✅ TERIMPLEMENTASI]`
- **MediaRepository & MediaPagingSource**: Pemutakhiran kueri `MediaStore.Images.Media` secara terhalaman dengan kriteria penyaringan utama `IS_TRASHED = 0`.
- **Coil Custom ImageLoader**: Pembatasan dekode thumbnail foto pada ukuran 256px dengan skema *memory cache* terisolasi via `CoilModule` untuk menjamin navigasi *scroll* hingga 120fps tanpa OOM.
- **Unit Test**: Teruji menggunakan `MediaPagingSourceTest` dengan cursor tiruan (*mock cursor*).

### 🔍 Modul 3: Full-Screen Photo Viewer `[✅ TERIMPLEMENTASI]`
- **HorizontalPager Viewer**: Peninjau foto layar penuh dengan navigasi *gesture swipe* antar foto secara halus (`MediaViewerScreen.kt`).
- **Pinch-to-Zoom & Double-Tap**: Penanganan gestur perbesaran foto (*zoom*) hingga resolusi asli via `ZoomableImage.kt`.
- **Metadata Bottom Sheet**: Menampilkan informasi detail foto (nama berkas, tanggal pengambilan, ukuran berkas, resolusi piksel) via `PhotoInfoBottomSheet.kt`.
- **Pintasan Editor**: Tombol akses cepat ke Editor Foto dari viewer (`ViewerViewModel.kt`).

### 📁 Modul 4: Pengelompokan Album (Buckets) `[✅ TERIMPLEMENTASI]`
- **Grouping Bucket**: Pengelompokan foto berdasarkan `BUCKET_DISPLAY_NAME` & `BUCKET_ID` dari MediaStore via `MediaRepository.getAlbums()`.
- **Tampilan Daftar Album**: Grid album (`AlbumListScreen.kt`) yang menyajikan foto sampul terbaru beserta jumlah total foto di dalamnya.
- **Album Detail Grid**: Layar grid terfilter khusus per album (`AlbumDetailScreen.kt`) dengan pengelolaan state terisolasi via `AlbumViewModel.kt`.

### 🎯 Modul 5: Aksi Multi-Select, Share & Scoped Storage Delete `[✅ TERIMPLEMENTASI]`
- **Multi-Select Mode**: Mode pemilihan banyak foto melalui *long-press* atau ketukan ikon centang pada grid foto (`SelectionTopAppBar.kt`).
- **Berbagi Media (Share)**: Berbagi massal media melalui intent `ACTION_SEND_MULTIPLE` lengkap dengan *URI permission grant*.
- **Penghapusan Scoped Storage**:
  - API 30+ (Android 11+): Memicu dialog konfirmasi resmi OS Android via `MediaStore.createDeleteRequest()`.
  - API 29 (Android 10): Menangkap `RecoverableSecurityException` dan mengarahkan pengguna ke konfirmasi izin.
  - API < 29: Penghapusan berkas langsung via izin *write storage* menggunakan `deletePhotosDirectly()`.

### 🎨 Modul 6: Custom Image Editor (Crop & Filter) `[✅ TERIMPLEMENTASI]`
- **Compose Canvas Crop**: Antarmuka pemotongan gambar berbasis gestur seret (*drag*) dan *zoom* dengan opsi rasio Bebas, 1:1, atau 4:3 (`ImageEditorScreen.kt`).
- **Real-Time ColorMatrix Filters**: Penerapan filter visual (Grayscale, Sepia, Warm, Cool, High Contrast) menggunakan `ColorMatrix` real-time (`EditorViewModel.kt`).
- **Save as New File**: Penulisan hasil edit sebagai berkas foto baru via `MediaStore.insert()` + `OutputStream` tanpa menguras memori (*inSampleSize*) dan menjaga foto asli tetap utuh.

### 🗑️ Modul 7: Trash / Recycle Bin Sistem `[✅ TERIMPLEMENTASI]`
- **System Trash Integration**: Memanfaatkan kolom `IS_TRASHED` bawaan sistem Android (API 30+) via `createTrashIntentSender()`.
- **Aksi Trash**: Memindahkan foto ke sistem trash via `MediaStore.createTrashRequest(uris, trash = true)`.
- **Layar Trash**: Menyajikan daftar foto terbuang (`TrashScreen.kt`) dengan opsi Pemulihan (*Restore*) (`trash = false`) atau Penghapusan Permanen (`createDeleteRequest()`).

### 🔒 Modul 8: Hidden Vault & App Lock `[✅ TERIMPLEMENTASI]`
- **Isolasi Berkas Privat**: Memindahkan foto tersembunyi dari direktori publik ke penyimpanan internal privat aplikasi (`filesDir/vault`), serta menghapus referensi asli dari `MediaStore`.
- **Hardware-Encrypted App Lock**: Pembukaan vault dilindungi oleh `BiometricPrompt` dengan opsi fallback PIN terenkripsi hardware via Encrypted DataStore (`androidx.security:security-crypto`).
- **Session-Only Decoding**: Thumbnail dalam vault hanya didekode ke dalam memori RAM selama sesi vault aktif dan tidak pernah ditulis ke direktori publik.
- **Restore to Gallery**: Mengembalikan foto dari vault ke galeri utama via `MediaStore.insert()` dan menghapus salinan privat.

### 🚀 Modul 9: Polish & Release (Signing Config & APK Production) `[✅ TERIMPLEMENTASI]`
- **Adaptive App Icon**: Ikon aplikasi adaptif (*Adaptive Icon*) dengan lapisan `foreground` + `background` khusus, label aplikasi "OP Gallery" yang konsisten di semua launcher Android.
- **Dark Theme Terintegrasi**: Dukungan tema gelap (*Dark Mode*) penuh via `MaterialTheme` Material 3, diaktifkan otomatis mengikuti preferensi sistem pengguna.
- **R8 Minification & ProGuard**: Pengoptimalan binary APK dengan eliminasi kode mati (*dead code elimination*) via `isMinifyEnabled = true` dan `isShrinkResources = true`, disertai aturan ProGuard khusus untuk Coil, Hilt, dan Encrypted DataStore.
- **Release Signing Config**: Konfigurasi penandatanganan digital APK (*signingConfig*) membaca kredensial keystore dari `local.properties` (non-Git) untuk keamanan penuh.
- **APK Release Siap Distribusi**: Output final `./gradlew assembleRelease` menghasilkan `app-release.apk` ter-sign dan teroptimasi, siap di-install di perangkat fisik atau diunggah ke Play Store.

### ⚙️ Modul 10: Settings (Pengaturan Aplikasi) `[✅ TERIMPLEMENTASI]`
- **Layar Pengaturan**: Antarmuka pengaturan preferensi aplikasi (`SettingsScreen.kt`) dengan state terisolasi via `SettingsViewModel.kt`.
- **Integrasi Hilt**: Modul Settings teregistrasi via `SettingsModule.kt` untuk penyediaan dependensi yang terisolasi.

---

## 💡 Konsep Logika Bisnis & Keamanan

1. **Scoped Storage Compliance**: Seluruh manipulasi berkas media eksternal wajib mematuhi aturan keamanan Android Scoped Storage. Aplikasi tidak boleh berasumsi memiliki akses tulis langsung ke media publik.
2. **Pencegahan Out Of Memory (OOM)**: Pemuatan gambar pada grid galeri dilarang menggunakan bitmap berukuran penuh (*full-size decode*). Dekode bitmap wajib disesuaikan dengan dimensi layar atau dipangkas ke 256px untuk thumbnail.
3. **Imutabilitas File Asli**: Proses pengeditan gambar tidak boleh menimpa berkas asli secara langsung. Hasil edit selalu disimpan sebagai entri berkas baru di `MediaStore`.

---

*Terakhir diperbarui: 28 Juli 2026 | v1.1.0 (versionCode 6) — Seluruh modul teroptimasi, berjalan lancar di perangkat fisik.*
