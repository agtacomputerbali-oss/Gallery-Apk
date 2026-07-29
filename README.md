# Gallery App — Android Media & Photo Vault

Aplikasi Galeri Android modern, performa tinggi, dan kaya fitur yang dibangun menggunakan **Kotlin**, **Jetpack Compose**, dan **Material 3**. Aplikasi ini dirancang dengan prinsip pemuatan cepat, navigasi mulus (*smooth scrolling*), pengelolaan file media yang aman (*Scoped Storage*), editor gambar terintegrasi, serta bilik rahasia (*Hidden Vault*) terenkripsi perangkat keras.

---

## 🚀 Fitur Utama

- 🎛️ **Filter Tipe Media Dinamis**: Filter universal di Floating Dock (`ALL`, `PHOTOS_ONLY`, `VIDEOS_ONLY`) dengan ikon & label dinamis di seluruh layar galeri.
- 📊 **Sort By & 1-Click Bulk Select**: Pengurutan dinamis (Terbaru, Terlama, Nama A-Z/Z-A, Ukuran) dan seleksi massal 1-klik via TopBar checklist & FloatingDock DoneAll.
- ⚓ **Floating Dock Navigation**: Floating Dock pill/capsule imersif dengan glassmorphism overlay di seluruh layar utama (Gallery, Albums, Trash, Vault, Settings), auto-hide saat scroll dan otomatis berubah menjadi Action Bar saat multi-select.
- ⚙️ **Settings Hub & Theme Customization**: Konfigurasi preferensi startup default filter, pemutaran video (auto-play & mute), penguncian otomatis Vault (Lock Delay), perubahan PIN terenkripsi, 4 warna aksen Material 3 (Emerald Green, Ocean Blue, Sunset Orange, Royal Purple), serta Cache Cleaner 1-klik.
- 🎨 **Advanced Photo Editor**: Canvas Live Preview real-time, grid overlay crop interaktif, penyetelan detail (Brightness, Contrast, Saturation, Warmth, Vignette), rotasi/flip, 10+ preset filter visual (Vivid, Noir, Cyberpunk, dll), doodle brush, text overlay, serta opsi ekspor format (JPEG/PNG/WEBP) dengan slider kualitas kompresi.
- 🏷️ **Smart Albums (Otomatis)**: Pengelompokan cerdas otomatis (Video, Screenshot, Memiliki Lokasi, Selfie & Kamera Depan) di layar Album.
- 🔍 **Duplicate Photo Finder**: Pencarian foto mirip & duplikat menggunakan algoritma *Discrete Cosine Transform (DCT) pHash 64-bit* dengan tombol **"✨ Bersihkan Semua" (One-tap Bulk Cleanup)** yang mempertahankan foto ukuran terbesar.
- 🎬 **Internal Video Player**: Pemutar video bawaan berbasis **Jetpack Media3 ExoPlayer** dengan kontrol playback penuh.
- 📸 **Grid Thumbnail & Fast Scroll**: Pemuatan berhalaman (*paginated*) galeri foto perangkat menggunakan Jetpack Paging 3 dan Coil cache tuning untuk mencegah kehabisan memori (*Out Of Memory*).
- 🔍 **Full-Screen Viewer**: Peninjau foto layar penuh dengan gesture *pinch-to-zoom*, *double-tap zoom*, dan navigasi *swipe* antar gambar (`HorizontalPager`).
- 📁 **Pengelompokan Album**: Pengelompokan media otomatis berdasarkan direktori/bucket (`BUCKET_DISPLAY_NAME`).
- 🎯 **Multi-Select & Berbagi**: Mode pemilihan banyak item untuk berbagi (*multi-share*) dan penghapusan massal berbasis Scoped Storage.
- 🗑️ **Trash / Recycle Bin Sistem**: Integrasi dengan kolom `IS_TRASHED` Scoped Storage Android (API 30+) untuk pemulihan (*restore*) atau penghapusan permanen.
- 🔒 **Hidden Vault & App Lock**: Sembunyikan foto ke penyimpanan internal privat dengan perlindungan autentikasi biometrik (`BiometricPrompt`) atau PIN terenkripsi.

---

## 🛠️ Technology Stack

| Komponen | Teknologi | Alasan & Deskripsi |
|---|---|---|
| **Bahasa** | Kotlin | Bahasa resmi utama Android dengan dukungan Coroutines & Flow |
| **Antarmuka UI** | Jetpack Compose + Material 3 | UI deklaratif modern dengan Floating Dock & sistem warna M3 |
| **Video Player** | Jetpack Media3 ExoPlayer | Pemutar video internal berkinerja tinggi dengan auto lifecycle cleanup |
| **Duplicate Finder** | Custom DCT pHash + Room v2 | Hashing 64-bit berbasis Bitmap API untuk deteksi duplikat offline |
| **Large List / Grid** | Jetpack Paging 3 (`paging-compose`) | Paginasi otomatis query `MediaStore` per chunk 30 item |
| **Image Loading** | Coil 2.6.0 (`coil-compose`) | Cache memori & disk khusus (thumbnail 256px) untuk cegah OOM |
| **Arsitektur** | MVVM + Repository + Hilt DI | Separation of concerns & Injeksi Dependensi terisolasi |
| **Async & Stream** | Coroutines + Flow | Eksekusi kueri `ContentResolver` di `Dispatchers.IO` |
| **Keamanan** | BiometricPrompt + Encrypted DataStore | Autentikasi biometrik & penyimpanan PIN terenkripsi hardware |
| **Minimal SDK** | API 26 (Android 8.0) | Mendukung >95% perangkat Android aktif |
| **Build System** | Gradle KTS + Version Catalog | Manajemen versi dependensi terpusat (`libs.versions.toml`) |

---

## 📋 Persyaratan Lingkungan Pengembangan

- **Android Studio**: Ladybug / Jellyfish (atau versi lebih baru)
- **JDK**: Java 17 / 21
- ⚡ **Local Caching & Indexing**: Room Database caching metadata MediaStore + WorkManager background indexing untuk initial load super cepat dan pencarian offline.
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 35 | App Version: 1.14.2 (versionCode 45)
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
│   │   ├── local/            # Room Database (GalleryDatabase, PhotoDao, CachedPhotoEntity)
│   │   ├── paging/           # MediaPagingSource (ContentResolver MediaStore query)
│   │   ├── repository/       # MediaRepositoryImpl, PhotoCacheRepositoryImpl, ThemeRepositoryImpl
│   │   └── worker/           # IndexingWorker & PHashIndexingWorker (WorkManager background task)
│   ├── domain/
│   │   ├── model/            # PhotoItem, Album, VaultItem, SmartAlbum, SortOption, MediaTypeFilter, SettingsPreferences
│   │   ├── repository/       # MediaRepository, PhotoCacheRepository, ThemeRepository (Contracts)
│   │   └── usecase/          # Use Cases (Delete, Share, HidePhotos, RestoreVault, SaveEdited, Copy/Move, dll)
│   ├── ui/
│   │   ├── main/             # AppNavigation.kt & MainActivity.kt (NavHost Setup)
│   │   ├── album/            # AlbumListScreen, AlbumDetailScreen, AlbumListViewModel
│   │   ├── components/       # SortBottomSheet, FloatingDock, FolderPickerBottomSheet, GridSectionHeader
│   │   ├── duplicate/        # DuplicateScreen & DuplicateViewModel (pHash DCT Finder)
│   │   ├── editor/           # EditorScreen & EditorViewModel (CropOverlay, Live Adjustments, Presets, Markup)
│   │   │   ├── components/   # CropOverlay.kt
│   │   │   └── model/        # EditorModels.kt
│   │   ├── gallery/          # GalleryHomeScreen & GalleryViewModel (Drag Selection, Pinch-to-Zoom)
│   │   ├── permission/       # PermissionScreen & PermissionViewModel (READ_MEDIA_IMAGES)
│   │   ├── settings/         # SettingsScreen & SettingsViewModel (Theme/Accent Color, Lock Delay, Cache Cleaner)
│   │   │   └── components/   # SettingsSectionCard.kt, AccentColorPicker.kt
│   │   ├── trash/            # TrashScreen & TrashViewModel (Scoped Storage Trash)
│   │   ├── vault/            # VaultScreen & VaultViewModel (BiometricAuthModal)
│   │   ├── video/            # VideoPlayerScreen & VideoPlayerViewModel (Media3 ExoPlayer)
│   │   ├── viewer/           # ViewerScreen, ZoomableImage, PhotoInfoBottomSheet
│   │   └── theme/            # Material 3 Color, Type, Shape & Theme
│   ├── di/                   # Hilt Modules (AppModule, DatabaseModule, DispatcherModule)
│   └── util/                 # BiometricHelper, PinEncryptionHelper, PHashCalculator
└── src/test/java/com/gallery/app/
    └── data/repository/      # Unit Tests
```

---

## 📄 Lisensi & Hak Cipta

Dokumentasi dan kode sumber dikembangkan untuk penggunaan internal enterprise.
*Terakhir diperbarui: 29 Juli 2026 | v1.14.2 (versionCode 45) — Release APK OPGallery-v1.14.2-release.apk.*
