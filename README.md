# Gallery App — Android Media & Photo Vault

Aplikasi Galeri Android modern, performa tinggi, dan kaya fitur yang dibangun menggunakan **Kotlin**, **Jetpack Compose**, dan **Material 3**. Aplikasi ini dirancang dengan prinsip pemuatan cepat, navigasi mulus (*smooth scrolling*), pengelolaan file media yang aman (*Scoped Storage*), editor gambar terintegrasi, serta bilik rahasia (*Hidden Vault*) terenkripsi perangkat keras.

---

## 🚀 Fitur Utama

- 📸 **Grid Thumbnail & Fast Scroll**: Pemuatan berhalaman (*paginated*) galeri foto perangkat menggunakan Jetpack Paging 3 dan Coil cache tuning untuk mencegah kehabisan memori (*Out Of Memory*).
- 🔍 **Full-Screen Viewer**: Peninjau foto layar penuh dengan gesture *pinch-to-zoom*, *double-tap zoom*, dan navigasi *swipe* antar gambar (`HorizontalPager`).
- 📁 **Pengelompokan Album**: Pengelompokan media otomatis berdasarkan direktori/bucket (`BUCKET_DISPLAY_NAME`).
- 🎯 **Multi-Select & Berbagi**: Mode pemilihan banyak item untuk berbagi (*multi-share*) dan penghapusan massal berbasis Scoped Storage.
- 🎨 **Editor Foto Terintegrasi**: Fitur potong gambar (*custom canvas crop*) dan filter visual real-time (`ColorMatrix`) tanpa mengubah file asli (disimpan sebagai file baru).
- 🗑️ **Trash / Recycle Bin Sistem**: Integrasi dengan kolom `IS_TRASHED` Scoped Storage Android (API 30+) untuk pemulihan (*restore*) atau penghapusan permanen.
- 🔒 **Hidden Vault & App Lock**: Sembunyikan foto ke penyimpanan internal privat dengan perlindungan autentikasi biometrik (`BiometricPrompt`) atau PIN terenkripsi.

---

## 🛠️ Technology Stack

| Komponen | Teknologi | Alasan & Deskripsi |
|---|---|---|
| **Bahasa** | Kotlin | Bahasa resmi utama Android dengan dukungan Coroutines & Flow |
| **Antarmuka UI** | Jetpack Compose + Material 3 | UI deklaratif modern dengan sistem warna & animasi M3 |
| **Large List / Grid** | Jetpack Paging 3 (`paging-compose`) | Paginasi otomatis query `MediaStore` per chunk 60 item |
| **Image Loading** | Coil 2 (`coil-compose`) | Cache memori & disk khusus (thumbnail 256px) untuk cegah OOM |
| **Arsitektur** | MVVM + Repository + Hilt DI | Separation of concerns & Injeksi Dependensi terisolasi |
| **Async & Stream** | Coroutines + Flow | Eksekusi kueri `ContentResolver` di `Dispatchers.IO` |
| **Keamanan** | BiometricPrompt + Encrypted DataStore | Autentikasi biometrik & penyimpanan PIN terenkripsi hardware |
| **Minimal SDK** | API 26 (Android 8.0) | Mendukung >95% perangkat Android aktif |
| **Build System** | Gradle KTS + Version Catalog | Manajemen versi dependensi terpusat (`libs.versions.toml`) |

---

## 📋 Persyaratan Lingkungan Pengembangan

- **Android Studio**: Ladybug / Jellyfish (atau versi lebih baru)
- **JDK**: Java 17 / 21
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 34/35
- **Gradle**: 8.x+ dengan Kotlin DSL

---

## ⚙️ Panduan Kompilasi & Pengujian

### 1. Build Variant Debug
Untuk mengompilasi dan menguji aplikasi secara lokal pada emulator atau perangkat fisik:
```bash
./gradlew assembleDebug
```

### 2. Jalankan Pengujian Unit (Unit Testing)
Untuk menjalankan suite pengujian otomatis pada repository dan PagingSource:
```bash
./gradlew test
```

### 3. Build Variant Release (APK Production)
Untuk menghasilkan berkas APK siap rilis:
```bash
./gradlew assembleRelease
```

---

## 🏗️ Arsitektur Proyek

Proyek ini menerapkan arsitektur **MVVM (Model-View-ViewModel)** dengan prinsip *Clean Architecture* sederhana:

```
app/
├── src/main/java/com/gallery/app/
│   ├── data/
│   │   ├── local/            # Encrypted DataStore & Internal File Vault Manager
│   │   ├── paging/           # MediaPagingSource (ContentResolver MediaStore query)
│   │   └── repository/       # MediaRepositoryImpl
│   ├── domain/
│   │   ├── model/            # PhotoItem, Album, VaultItem
│   │   ├── repository/       # MediaRepository (Interface & Contracts)
│   │   └── usecase/          # Business logic (Crop, Filter, Trash, Vault)
│   ├── ui/
│   │   ├── album/            # AlbumListScreen, AlbumDetailScreen, AlbumViewModel
│   │   ├── components/       # SelectionTopAppBar & Reusable Compose Components
│   │   ├── editor/           # ImageEditorScreen & EditorViewModel (Compose Canvas Crop & ColorMatrix)
│   │   ├── gallery/          # GalleryGridScreen & GalleryViewModel
│   │   ├── permission/       # PermissionScreen & PermissionState (READ_MEDIA_IMAGES)
│   │   ├── trash/            # TrashScreen & TrashViewModel (Scoped Storage Trash)
│   │   ├── vault/            # HiddenVaultScreen, BiometricAuthModal
│   │   ├── viewer/           # MediaViewerScreen, ZoomableImage, PhotoInfoBottomSheet
│   │   └── theme/            # Material 3 Color, Type, Shape & Theme
│   └── di/                   # Hilt Modules (AppModule, CoilModule, RepositoryModule)
└── src/test/java/com/gallery/app/
    └── data/paging/          # MediaPagingSourceTest (Unit Tests)
```

---

## 📄 Lisensi & Hak Cipta

Dokumentasi dan kode sumber dikembangkan untuk penggunaan internal enterprise.
*Terakhir diperbarui: 28 Juli 2026*
