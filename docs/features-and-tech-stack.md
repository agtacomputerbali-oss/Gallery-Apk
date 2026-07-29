# 📚 Dokumentasi Fitur & Tech Stack — Gallery App Android

Dokumen ini berisi informasi komprehensif mengenai daftar modul dan fitur yang telah dikembangkan dalam aplikasi **Gallery App Android**, serta rincian *technology stack* (tech stack) yang digunakan untuk membangun proyek ini.

> **Versi Dokumen:** v1.14.2 (versionCode 45) — 29 Juli 2026 | Android Enterprise Application

---

## 🛠️ Technology Stack

Aplikasi ini dibangun menggunakan arsitektur modern Android berbasis **Kotlin** dan **Jetpack Compose** yang berfokus pada kecepatan pemrosesan berkas media, penggunaan memori yang efisien, dan keamanan data tingkat tinggi. Berikut adalah komponen *tech stack* yang digunakan:

### 1. Core Framework & Bahasa
* **Kotlin (v1.9+)**: Bahasa pemrograman utama yang menawarkan sintaks modern, keamanan pengetikan statis (*null-safety*), dan integrasi Coroutines yang efisien.
* **Jetpack Compose**: Framework UI deklaratif modern buatan Google untuk membangun antarmuka pengguna Android yang cepat, responsif, dan fleksibel tanpa menggunakan XML layouts.
* **Lifecycle Runtime Compose (`androidx.lifecycle:lifecycle-runtime-compose`)**: Pengambilan data `StateFlow` secara *lifecycle-aware* (`collectAsStateWithLifecycle`) untuk menghentikan rekomposisi otomatis saat aplikasi berada di background.
* **Material Design 3 (Material You)**: Komponen UI dasar yang aksesibel, modern, dan mendukung tema adaptif dinamis (*Dynamic Color*).

### 2. Multimedia & Player
* **Jetpack Media3 ExoPlayer (`androidx.media3:media3-exoplayer`)**: Pemutar video internal berkinerja tinggi yang mendukung pemutaran berbagai format media (MP4, MKV, WebM) dengan pengelolaan memori otomatis (`player.release()`).

### 3. Paginasi & Pengelolaan Media
* **Jetpack Paging 3 (`androidx.paging:paging-compose`)**: Mengelola kueri berkas foto dari `MediaStore` secara bertahap (*chunking* 30 item per halaman, `initialLoadSize = 60`, `prefetchDistance = 15`, `maxSize = 200`) untuk mencegah masalah kehabisan memori (*Out Of Memory / OOM*) pada galeri berkapasitas ribuan foto.
* **Coil (`coil-compose v2.6.0`)**: Pustaka pemuat gambar (*image loader*) berbasis Kotlin Coroutines. Dikonfigurasi secara terpusat pada `GalleryApplication` dengan *MemoryCache* (30% heap) dan *DiskCache* (5% disk) terukur, serta *ImageLoader* terisolasi tanpa cache khusus untuk Vault.
* **Room Database & WorkManager**: Pengindeksan metadata lokal (`CachedPhotoEntity`) dan komputasi *Perceptual Hashing (pHash)* di latar belakang tanpa memblokir thread UI.

### 3. Arsitektur & Dependency Injection
* **MVVM (Model-View-ViewModel)**: Pola arsitektur resmi Android untuk memisahkan logika bisnis dari lapisan UI.
* **Hilt (Jetpack Dependency Injection)**: Framework injeksi dependensi terpusat (`AppModule`, `SettingsModule`) untuk mengelola siklus hidup ViewModel, Repository, dan Service.
* **Coroutines & Flow**: Pemrograman asinkron berbasis aliran data (*reactive stream*) untuk mengeksecusi kueri `ContentResolver` pada thread `Dispatchers.IO` tanpa mengganggu kelancaran UI (60fps/120fps UI thread).

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
- **Multi-Version Runtime Permissions**: Mendukung pemanggilan izin `READ_MEDIA_IMAGES` dan `READ_MEDIA_VIDEO` untuk Android 13+ (API 33+) serta fallback `READ_EXTERNAL_STORAGE` untuk versi Android 8.0 - 12 (API 26–32).
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
- **Album Detail Grid**: Layar grid terfilter khusus per album (`AlbumDetailScreen.kt`) dengan pengelolaan state terisolasi via `AlbumDetailViewModel.kt`, dilengkapi `FloatingDockContainer` dan fitur *Multi-Select*.

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
- **Manual Index Sync**: Ditambahkan kartu status jumlah cache metadata foto di Room DB dan tombol manual trigger `Sinkronkan Ulang Index`.

### ⚡ Modul 11: Foundation & Local Caching (Fase P1) `[✅ TERIMPLEMENTASI]`
- **Room Database Caching (`androidx.room`)**: Metadata foto (`MediaStore`) di-index secara lokal di Room Database (`GalleryDatabase`, `CachedPhotoEntity`, `PhotoDao`) termasuk kolom EXIF GPS (`latitude`, `longitude`) untuk persiapan P2.
- **WorkManager Background Indexing (`androidx.work`)**: Indexing latar belakang berbasis `CoroutineWorker` (`IndexingWorker`) dengan constraint `setRequiresBatteryNotLow(true)` agar tidak memblokir main UI thread (120fps).
- **Reaktif ContentObserver**: Pemantauan perubahan `MediaStore` secara real-time via `MediaStoreObserver` yang memicu pemutakhiran cache inkremental otomatis (debounced).

### 🔍 Modul 12: Duplicate Photo Finder & Bulk Cleanup (pHash DCT) `[✅ TERIMPLEMENTASI]`
- **DCT 64-bit Perceptual Hashing**: Algoritma kemiripan visual berbasis Discrete Cosine Transform (DCT) 64-bit (`PHashCalculator.kt`) untuk menghitung pHash bitmap tanpa library eksternal tambahan.
- **Room Migration (v1 → v2)**: Skema Room Database mendukung kolom `pHash` dan indeks `index_cached_photos_pHash` untuk perbandingan Hamming distance offline cepat (threshold Hamming ≤ 10).
- **One-tap Bulk Cleanup**: Tombol "✨ Bersihkan Semua" di `DuplicateScreen.kt` yang secara otomatis mempertahankan 1 foto kualitas terbaik (ukuran terbesar) per grup duplikat dan memindahkan sisa duplikat ke System Trash via `MediaStore.createTrashRequest()`.

### 🎬 Modul 13: Internal Video Player (Jetpack Media3 ExoPlayer) `[✅ TERIMPLEMENTASI]`
- **Jetpack Media3 ExoPlayer Integration**: Pemutaran video berkinerja tinggi berbasis `AndroidView` + `PlayerView` di `VideoPlayerScreen.kt`.
- **Auto Lifecycle Cleanup**: Pelepasan instance ExoPlayer secara otomatis (`player.release()`) menggunakan `DisposableEffect` saat berpindah layar untuk mencegah kebocoran memori.

### ⚓ Modul 14: Floating Dock Navigation & Action Mode `[✅ TERIMPLEMENTASI]`
- **Floating Glassmorphism Pill**: Komponen navigasi melayang kapsul (`FloatingDock.kt`) di layar utama (Gallery, Albums, Trash, Vault, Settings) dengan efek transparansi glassmorphism.
- **Auto-Hide & Mode Selection**: Melayang secara halus saat scroll grid dan bertransisi menjadi Action Bar interaktif saat mode multi-select diaktifkan.

### 📊 Modul 15: Fitur "Sort By" & "1-Click Bulk Select" `[✅ TERIMPLEMENTASI]`
- **Pengurutan Dinamis Media (Sort By)**: Opsi pengurutan media di Galeri Utama & Detail Album (`SortBottomSheet.kt` + `SortOption.kt`) yang mendukung *Terbaru*, *Terlama*, *Nama (A-Z/Z-A)*, dan *Ukuran (Terbesar/Terkecil)* dengan pemutakhiran Paging 3 instan.
- **1-Click Bulk Select (Select All)**: Aktivasi mode seleksi 1-klik via ikon checklist di Top Bar dan aksi *Pilih Semua / Batal Pilih* 1-klik di `FloatingDockActionBar`.

### 🛡️ Modul 16: E2E Security & Stability Hardening `[✅ TERIMPLEMENTASI]`
- **Hilt @IoDispatcher & @VaultImageLoader**: Injeksi CoroutineDispatcher terpusat untuk operasi I/O dan isolasi ImageLoader Vault tanpa cache publik.
- **Vault PIN Lockout & Biometric Scope**: Batas 5 kali percobaan salah PIN dengan 30-detik lockout backoff di EncryptedSharedPreferences, serta eksekusi callback BiometricPrompt terikat viewModelScope.
- **Worker & Storage Safety**: Penyaringan IS_TRASHED = 0 pada IndexingWorker, try-finally bitmap recycling pada PHashIndexingWorker, dan penanganan event decoupled via SharedFlow di TrashViewModel.

### 🔒 Modul 17: Preservasi Struktur Folder & Restorasi Folder Asal di Hidden Vault `[✅ TERIMPLEMENTASI]`
- **Pencatatan Metadata Folder Asal**: Menyimpan nama folder (`folderName`) dan relative path (`relativePath`) di metadata privat Vault (`vault_index.json`) saat menyembunyikan album/foto.
- **Tampilan Grid Folder Vault**: Antarmuka `VaultScreen` dilengkapi Tab/Filter ("Semua Foto" vs "Folder") serta kartu folder (`VaultFolderCard`) untuk menavigasi foto tersembunyi per album/folder.
- **Restorasi ke Folder Asal**: Mengembalikan foto dari Vault langsung ke direktori asalnya (`Pictures/<folderName>`) saat tombol Restore dipicu, tanpa menumpuknya di direktori umum.
- **Kompatibilitas Mundur (Legacy Items)**: Memastikan berkas vault lama tanpa metadata folder tetap dapat dibaca dan dikembalikan dengan aman tanpa crash.

### 📐 Modul 18: Pengelompokan Seksi (Bulan/Tahun), Bulk Select Seksi, Grid 3-6 Kolom & Gestur Pinch `[✅ TERIMPLEMENTASI]`
- **Section Headers & Bulk Select 1-Klik**: Pembagi seksi waktu (misal: "Juli 2026 • 15 foto") di Galeri Utama, Detail Album, dan Hidden Vault dengan ikon checklist di kanan header untuk menyeleksi 100% foto dalam seksi tersebut secara instan.
- **Sort Options Baru (Bulan & Tahun)**: Opsi pengurutan `MONTH_DESC` ("Bulan & Tahun") dan `YEAR_DESC` ("Tahun") pada `SortOption.kt`, `SortBottomSheet.kt`, serta penambahan fitur Sort lengkap di `VaultScreen.kt`.
- **Pengaturan Kolom Grid Individu Per Layar**: Jumlah kolom grid (3, 4, 5, 6 kolom) yang disimpan secara independen per layar di DataStore (`key_gallery_grid_columns`, `key_album_grid_columns`, `key_vault_grid_columns`).
- **Quick Toggle TopBar & Gestur Pinch Zoom**: Ikon tombol pintas di TopBar (`GridColumnToggleButton`) dan gestur Pinch-in / Pinch-out pada grid media untuk mengubah jumlah kolom grid secara instan.

### 📁 Modul 19: Manajemen Berkas (Copy to, Move to, Create Folder & Delete Clarity) `[✅ TERIMPLEMENTASI]`
- **Aksi Delete Konsisten**: Tombol "Delete" di `FloatingDock` dipastikan selalu aktif dan responsif pada mode seleksi tunggal (1 foto) maupun seleksi massal (banyak foto) di Galeri Utama, Album Detail, dan Sampah.
- **Salin & Pindah Media (Copy to & Move to)**: Aksi `onCopyTo` dan `onMoveTo` di `FloatingDockActionBar` memicu `FolderPickerBottomSheet` untuk menyalin atau memindahkan foto ke album/folder tujuan via `ContentResolver` I/O di `Dispatchers.IO`.
- **Buat Folder Baru Inline**: Dialog `FolderPickerBottomSheet` dan antarmuka `AlbumListScreen` dilengkapi tombol `+ Buat Folder Baru` via `CreateFolderAlertDialog` dengan validasi karakter ilegal. Folder dibuat secara aman tanpa izin berbahaya `MANAGE_EXTERNAL_STORAGE` menggunakan `ContentValues` MediaStore.
- **Auto-Refresh pada Penyembunyian (Hide)**: Pemutakhiran otomatis antarmuka pada `GalleryHomeScreen` dan `AlbumListScreen` ketika foto atau album disembunyikan ke Vault, menjamin item yang disembunyikan seketika hilang dari layar tanpa aksi manual pengguna.
- **Vault Restorasi Otomatis (Folder Re-Creation)**: Mengembalikan foto dari Vault ke direktori asalnya (`Pictures/<folderAsal>/`). Jika folder fisik asal telah terhapus, MediaStore secara otomatis membuat kembali folder dengan nama yang sama.

### 🛡️ Modul 20: Audit Bug Fixes & Stability Hardening (v1.9.7) `[✅ TERIMPLEMENTASI]`
- **Keamanan & Resiliensi Vault**: Penanganan try-catch fallback pada `PinEncryptionHelper` & `VaultViewModel.checkSecurityState()` untuk mencegah `SecurityException` fatal jika hardware Keystore / EncryptedSharedPreferences ter-reset.
- **Mutasi State Atomic**: `VaultViewModel.submitPin()` disempurnakan menggunakan `_uiState.update {}` atomic tanpa race condition.
- **Kepatuhan MediaStore API 29+**: `IndexingWorker` menghapus query kolom deprecated `LATITUDE`/`LONGITUDE`, mencegah `IllegalArgumentException` pada Android API 29+ (MIUI, ColorOS, Samsung One UI).
- **Pencegahan OOM Coil**: Grid thumbnail di `PhotoThumbnail` dan `VaultScreen` menyertakan `.size(256)` dan `.precision(INEXACT)` secara eksplisit untuk mencegah Out Of Memory.
- **Move Photo Delete Intent**: `movePhotosToFolder` mengembalikan `NeedsDeleteConfirmation(intentSender, count, targetFolderName)` untuk meluncurkan dialog sistem konfirmasi hapus foto asal pada Scoped Storage Android 11+.

### ⚡ Modul 21: Section Headers, Drag Selection, Pinch-to-Zoom & Horizontal Scroll Floating Dock (v1.9.11) `[✅ TERIMPLEMENTASI]`
- **Section Headers Paging 3**: Integrasi `insertSeparators` pada `PagingData` stream di `GalleryViewModel.kt` untuk menampilkan `GridSectionHeader` waktu (Tanggal / Bulan / Tahun) di `GalleryHomeScreen.kt`.
- **Drag-to-Select (Click and Swap)**: Penambahan gestur 1-jari `detectDragGesturesAfterLongPress` di `GalleryHomeScreen.kt` untuk menyeleksi banyak foto secara cepat melintasi usapan jari.
- **Pinch-to-Zoom Gesture Coexistence**: Gestur 2-jari `detectTransformGestures` untuk mengubah jumlah kolom grid (3 s/d 5 kolom di Smartphone, hingga 8 kolom di Tablet) diisolasi dengan lancar dan memberikan getaran *Haptic Feedback*.
- **Horizontal Scroll Floating Dock**: Menambahkan `Modifier.horizontalScroll(rememberScrollState())` pada `FloatingDockActionBar` di `FloatingDock.kt` sehingga 100% tombol aksi (Select All, Share, Copy, Move, Vault, Delete) dapat di-scroll dan diakses di semua ukuran layar tanpa terpotong.

### 🎛️ Modul 22: Universal Media Type Filtering di Floating Dock `[✅ TERIMPLEMENTASI]`
- **Filter Type Control**: Transformasi tombol pertama Floating Dock dari tombol static "Foto" menjadi **Media Type Filter Control** (`ALL`, `PHOTOS_ONLY`, `VIDEOS_ONLY`) dengan ikon (`PermMedia`, `Image`, `Videocam`) dan label dinamis (`Semua`, `Foto`, `Video`).
- **Kueri Unified MediaStore**: Integrasi `MediaPagingSource` dengan `MediaStore.Files.getContentUri("external")` untuk mendukung penggabungan kueri `MEDIA_TYPE_IMAGE` dan `MEDIA_TYPE_VIDEO` secara transparan.
- **Penerapan Universal**: Penyaringan tipe media berlaku secara konsisten di Galeri Utama (`GalleryHomeScreen`), Detail Album (`AlbumDetailScreen`), dan Hidden Vault (`VaultScreen`).

### ⚙️ Modul 23: Fitur Settings Hub & Kustomisasi Perangkat `[✅ TERIMPLEMENTASI]`
- **DataStore Preferences**: Pengelolaan preferensi aplikasi terpusat via `SettingsPreferences` dan `ThemeRepositoryImpl` tersimpan di DataStore.
- **Preferensi Video & Vault**: Opsi penentuan filter awal saat startup, sakelar *Auto-play Video Preview*, *Mute Video by Default*, tenggang waktu penguncian otomatis Vault (*Lock Delay*: Immediately, 30s, 1m, 5m), dan dialog ubah PIN terenkripsi.
- **Kustomisasi Tema & Warna Aksen Material 3**: Opsi Mode Tema (Ikuti Sistem / Terang / Gelap) serta 4 pilihan warna aksen Material 3 (Emerald Green, Ocean Blue, Sunset Orange, Royal Purple) yang mengubah `ColorScheme` aplikasi secara reaktif.
- **Storage & Cache Cleaner Hub**: Menyajikan statistik pemakaian disk (Room DB index, Coil disk cache, Vault storage) dan menyediakan tombol 1-klik `Bersihkan Cache Thumbnail` untuk mengosongkan memori cache tanpa menghapus foto asli.

### 🎨 Modul 24: Professional Photo Editor (Adjustments, Presets, Markup & Export) `[✅ TERIMPLEMENTASI]`
- **Real-Time Live Canvas Preview**: Visualisasi pengeditan secara real-time via `ColorMatrixColorFilter`, `graphicsLayer` rotation/flip, `CropOverlay`, serta lukisan doodle dan teks di `EditorScreen.kt`.
- **Advanced Adjustments**: Penyesuaian presisi slider -100 s/d +100 untuk Brightness, Contrast, Saturation, Warmth/Temperature, dan Vignette.
- **Transformasi & 10+ Visual Presets**: Rotasi 90° Searah/Berlawanan Jarum Jam, Flip Horizontal/Vertical, serta 10+ preset filter (*Original, Vivid, Warm Vintage, Cool Breeze, Dramatic B&W, Noir, Sepia, Cinematic, Pastel, Cyberpunk*).
- **Doodle Brush & Text Markup**: Lukisan kuas bebas dengan pilihan warna (*Color Picker*) & ketebalan garis (*Stroke Width*), disertai overlay penambahan teks kustom.
- **Ekspor Format & Kualitas**: Dialog ekspor yang mendukung format `JPEG`, `PNG`, `WEBP` dengan slider kualitas kompresi `50%` s/d `100%`, disimpan ke folder asal media (`RELATIVE_PATH`) tanpa merusak foto asli.

### 📱 Modul 25: Android 15 Edge-to-Edge Support & Target SDK 35 Upgrade `[✅ TERIMPLEMENTASI]`
- **Target SDK 35 Compliance**: Pengkinian `compileSdk = 35` dan `targetSdk = 35` di `app/build.gradle.kts`.
- **Edge-to-Edge Rendering**: Penerapan `enableEdgeToEdge()` di `MainActivity.kt` dengan penanganan insets Material 3 Surface & Scaffolding agar tampilan antarmuka transparan sempurna pada Android 15.

---

## 💡 Konsep Logika Bisnis & Keamanan

1. **Scoped Storage Compliance**: Seluruh manipulasi berkas media eksternal wajib mematuhi aturan keamanan Android Scoped Storage. Aplikasi tidak boleh berasumsi memiliki akses tulis langsung ke media publik.
2. **Pencegahan Out Of Memory (OOM)**: Pemuatan gambar pada grid galeri dilarang menggunakan bitmap berukuran penuh (*full-size decode*). Dekode bitmap wajib disesuaikan dengan dimensi layar atau dipangkas ke 256px untuk thumbnail.
3. **Imutabilitas File Asli**: Proses pengeditan gambar tidak boleh menimpa berkas asli secara langsung. Hasil edit selalu disimpan sebagai entri berkas baru di `MediaStore`.

---

*Terakhir diperbarui: 29 Juli 2026 | v1.14.2 (versionCode 45) — Release APK OPGallery-v1.14.2-release.apk terkompilasi.*
