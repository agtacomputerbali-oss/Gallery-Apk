# Developer Guide — Gallery App Android

> **Versi:** v1.14.2 — 29 Juli 2026
> **Tujuan:** Panduan referensi cepat bagi pengembang dan perkakas otomatis untuk memahami arsitektur, alur data, keamanan Scoped Storage, dan proses kompilasi aplikasi galeri Android ini.

---

## Daftar Isi

1. [File Map](#1-file-map)
2. [Data Flow](#2-data-flow)
3. [Permission & Auth Flow](#3-permission--auth-flow)
4. [Risk Map](#4-risk-map)
5. [Change Guide](#5-change-guide)

---

## 1. File Map

Gambaran lengkap struktur direktori dan tanggung jawab setiap paket dalam proyek Android ini.

```
Gallery-Apk/
│
├── app/                                      ← Source code modul utama aplikasi Android
│   ├── src/main/
│   │   ├── java/com/gallery/app/             ← Berkas kode Kotlin
│   │   │   ├── data/                         ← Lapisan Data (MediaStore, Room & Vault Storage)
│   │   │   │   ├── local/                    ← Room Database Caching & pHash
│   │   │   │   │   ├── dao/PhotoDao.kt
│   │   │   │   │   ├── entity/CachedPhotoEntity.kt
│   │   │   │   │   └── GalleryDatabase.kt
│   │   │   │   ├── paging/                   ← Jetpack Paging 3 Source (Dynamic Sort & Media Filter)
│   │   │   │   │   └── MediaPagingSource.kt
│   │   │   │   ├── repository/               ← Implementasi Repository
│   │   │   │   │   ├── MediaRepositoryImpl.kt
│   │   │   │   │   ├── PhotoCacheRepositoryImpl.kt
│   │   │   │   │   └── ThemeRepositoryImpl.kt
│   │   │   │   └── worker/                   ← WorkManager & Observer
│   │   │   │       ├── IndexingWorker.kt
│   │   │   │       ├── PHashIndexingWorker.kt
│   │   │   │       └── MediaStoreObserver.kt
│   │   │   │
│   │   │   ├── domain/                       ← Lapisan Domain (Model & Interface)
│   │   │   │   ├── model/                    ← Data Class & Enums
│   │   │   │   │   ├── PhotoItem.kt
│   │   │   │   │   ├── Album.kt
│   │   │   │   │   ├── VaultItem.kt
│   │   │   │   │   ├── SmartAlbum.kt
│   │   │   │   │   ├── SortOption.kt
│   │   │   │   │   ├── MediaTypeFilter.kt
│   │   │   │   │   ├── SettingsPreferences.kt
│   │   │   │   │   └── FolderItem.kt
│   │   │   │   ├── repository/               ← Contracts
│   │   │   │   │   ├── MediaRepository.kt
│   │   │   │   │   ├── PhotoCacheRepository.kt
│   │   │   │   │   └── ThemeRepository.kt
│   │   │   │   └── usecase/                  ← Logika Bisnis (Use Cases)
│   │   │   │       ├── DeletePhotosUseCase.kt
│   │   │   │       ├── GetAlbumsUseCase.kt
│   │   │   │       ├── GetPhotosByBucketUseCase.kt
│   │   │   │       ├── HidePhotosUseCase.kt
│   │   │   │       ├── PermanentDeleteUseCase.kt
│   │   │   │       ├── RestoreFromVaultUseCase.kt
│   │   │   │       ├── RestorePhotosUseCase.kt
│   │   │   │       ├── SaveEditedPhotoUseCase.kt
│   │   │   │       ├── CopyPhotosUseCase.kt
│   │   │   │       ├── MovePhotosUseCase.kt
│   │   │   │       ├── CreateFolderUseCase.kt
│   │   │   │       └── SharePhotosUseCase.kt
│   │   │   │
│   │   │   ├── ui/                           ← Lapisan Antarmuka Jetpack Compose
│   │   │   │   ├── main/                     ← Entry Point & Navigasi Utama
│   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   └── MainActivity.kt
│   │   │   │   ├── components/               ← Komponen UI Reusable
│   │   │   │   │   ├── SortBottomSheet.kt
│   │   │   │   │   ├── FloatingDock.kt
│   │   │   │   │   ├── FolderPickerBottomSheet.kt
│   │   │   │   │   └── GridSectionHeader.kt
│   │   │   │   ├── gallery/                  ← Layar Utama Galeri Grid
│   │   │   │   │   ├── GalleryHomeScreen.kt
│   │   │   │   │   └── GalleryViewModel.kt
│   │   │   │   ├── viewer/                   ← Peninjau Foto Screen Penuh & Zoom
│   │   │   │   │   ├── ViewerScreen.kt
│   │   │   │   │   ├── PhotoInfoBottomSheet.kt
│   │   │   │   │   ├── ViewerViewModel.kt
│   │   │   │   │   └── ZoomableImage.kt
│   │   │   │   ├── album/                    ← Pengelompokan & Grid Album
│   │   │   │   │   ├── AlbumListScreen.kt
│   │   │   │   │   ├── AlbumDetailScreen.kt
│   │   │   │   │   └── AlbumListViewModel.kt
│   │   │   │   ├── duplicate/                ← Layar Deteksi Duplikat pHash
│   │   │   │   │   ├── DuplicateScreen.kt
│   │   │   │   │   └── DuplicateViewModel.kt
│   │   │   │   ├── video/                    ← Layar Pemutar Video Media3
│   │   │   │   │   ├── VideoPlayerScreen.kt
│   │   │   │   │   └── VideoPlayerViewModel.kt
│   │   │   │   ├── permission/               ← Layar Penanganan Izin Media
│   │   │   │   │   ├── PermissionScreen.kt
│   │   │   │   │   └── PermissionViewModel.kt
│   │   │   │   ├── editor/                   ← Layar Advanced Photo Editor
│   │   │   │   │   ├── EditorScreen.kt
│   │   │   │   │   ├── EditorViewModel.kt
│   │   │   │   │   ├── components/CropOverlay.kt
│   │   │   │   │   └── model/EditorModels.kt
│   │   │   │   ├── trash/                    ← Layar Sampah Sistem
│   │   │   │   │   ├── TrashScreen.kt
│   │   │   │   │   └── TrashViewModel.kt
│   │   │   │   ├── vault/                    ← Layar Bilik Rahasia & Biometrik
│   │   │   │   │   ├── VaultScreen.kt
│   │   │   │   │   ├── VaultViewModel.kt
│   │   │   │   │   └── BiometricAuthModal.kt
│   │   │   │   ├── settings/                 ← Layar Pengaturan Aplikasi
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   │       ├── SettingsSectionCard.kt
│   │   │   │   │       └── AccentColorPicker.kt
│   │   │   │   └── theme/                    ← Material 3 Color, Type, Shape & Theme
│   │   │   │
│   │   │   ├── di/                           ← Module Hilt Dependency Injection
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   └── DispatcherModule.kt
│   │   │   │
│   │   │   ├── util/                         ← Utility & Helper
│   │   │   │   ├── BiometricHelper.kt
│   │   │   │   ├── PinEncryptionHelper.kt
│   │   │   │   └── PHashCalculator.kt
│   │   │   │
│   │   │   ├── GalleryApplication.kt
│   │   │
│   │   └── AndroidManifest.xml               ← Deklarasi Permissions & Main Activity
│   │
│   └── src/test/java/com/gallery/app/         ← Pengujian Unit (Unit Tests)
│       └── data/repository/
│           └── MediaRepositoryImplTest.kt
│
├── gradle/
│   └── libs.versions.toml                    ← Version Catalog (Centralized dependency versions)
│
├── docs/                                     ← Dokumentasi Resmi Proyek
│   ├── architecture-decisions.md             ← ADR-001 s/d ADR-017
│   ├── features-and-tech-stack.md            ← Fitur Modul 1 s/d 25 & Tech Stack lengkap
│   ├── ui-design-guidelines.md               ← Standar Design System Jetpack Compose M3
│   ├── production-deployment.md              ← Panduan Build & Signing Release APK
│   └── developer-guide.md                    ← Dokumen ini: File Map, Data Flow, Risk Map
│
├── build.gradle.kts                          ← Root gradle build script
├── settings.gradle.kts                       ← Gradle settings
└── README.md                                 ← Panduan proyek utama
```


---

## 2. Data Flow

### 2.1 Alur Pengambilan Foto Galeri (MediaStore → Paging 3 → Compose)

```
┌─────────────────────────────────────────────────────────────────────────┐
│  MEDIASTORE (Android ContentResolver)                                    │
│  MediaStore.Images.Media.EXTERNAL_CONTENT_URI (IS_TRASHED = 0)           │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │  Dispatchers.IO
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  MediaPagingSource (Paging 3)                                           │
│  - Membaca cursor per 60 item                                            │
│  - Memetakan ke PhotoItem(id, uri, dateAdded, bucketName, size)        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  MediaRepositoryImpl                                                    │
│  - Flow<PagingData<PhotoItem>>                                          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  GalleryViewModel                                                       │
│  - stateIn() / cachedIn(viewModelScope)                                 │
│  - Expose UI state via StateFlow<PagingData<PhotoItem>>                 │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  JETPACK COMPOSE UI (GalleryGridScreen)                                 │
│  - collectAsLazyPagingItems()                                           │
│  - LazyVerticalGrid + Coil 3 Subsampling (size 256px thumbnail)         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Alur Local Metadata Caching (MediaStore ➔ Observer ➔ WorkManager ➔ Room DB)

```
[MEDIASTORE CHANGE / EVENT]
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  MediaStoreObserver (ContentObserver)                                    │
│  - Register di Application.onCreate()                                   │
│  - onChange() mendeteksi perubahan MediaStore                            │
│  - Enqueue debounced IndexingWorker (3s delay)                          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Dispatchers.IO
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  IndexingWorker (WorkManager CoroutineWorker)                           │
│  - Constraint: setRequiresBatteryNotLow(true)                           │
│  - Query MediaStore.Images.Media (termasuk EXIF GPS latitude/longitude) │
│  - Batch upsert ke Room via PhotoDao.upsertPhotos()                     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  ROOM DATABASE (GalleryDatabase -> PhotoDao -> CachedPhotoEntity)       │
│  - Table: cached_photos (IS_TRASHED = 0 / 1, pHash TEXT)                 │
│  - Flow<Int> photoCount -> SettingsViewModel (UI SettingsScreen)        │
│  - PagingSource<Int, CachedPhotoEntity> -> PhotoCacheRepositoryImpl     │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Alur Deteksi Foto Duplikat (Room DB ➔ PHashIndexingWorker ➔ PHashCalculator ➔ DuplicateScreen)

```
[ROOM DB CACHED PHOTOS WITHOUT PHASH]
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHashIndexingWorker (WorkManager Background Worker)                     │
│  - Query photo entries with pHash IS NULL                               │
│  - Compute 64-bit DCT pHash via PHashCalculator.kt in Dispatchers.IO    │
│  - Update pHash column in Room DB                                       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  DuplicateViewModel / PhotoCacheRepository                              │
│  - Compare Hamming distance between 64-bit pHash strings (threshold ≤ 10)│
│  - Group duplicate photos & mark largest size as "Best Quality"        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  JETPACK COMPOSE UI (DuplicateScreen)                                   │
│  - Display group cards with "Best Quality" badge                        │
│  - Trigger One-tap Bulk Cleanup via MediaStore.createTrashRequest()     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Permission & Auth Flow


### 3.1 Diagram Alur Izin Akses Media & Autentikasi Vault

```
PENGGUNA MEMBUKA APLIKASI
         │
         ▼
 ┌──────────────┐
 │ Cek Android  │
 │ OS Version   │
 └──────┬───────┘
        │
        ├── Android 13+ (API 33+) ──> minta `READ_MEDIA_IMAGES`
        │
        └── Android 8-12 (API 26-32) ─> minta `READ_EXTERNAL_STORAGE`
        │
        ├── ✅ Diberikan ──> Load GalleryGridScreen
        │
        └── ❌ Ditolak   ──> Tampilkan Denied State + Tombol Pintasan ke Settings

MEMBUKA HIDDEN VAULT
         │
         ▼
 ┌─────────────────────────────────────────────────────────┐
 │ BiometricPrompt (Fingerprint / Face Recognition)        │
 │                                                         │
 │ ├── ✅ Sukses  ──> Dekode VaultItem di Memory RAM       │
 │ │                  (Buka Layar HiddenVaultScreen)       │
 │ │                                                         │
 │ └── ❌ Gagal   ──> Minta Fallback PIN Terenkripsi      │
 │                    (Divalidasi via Encrypted DataStore) │
 └─────────────────────────────────────────────────────────┘
```

---

## 4. Risk Map

Tabel konsolidasi risiko teknis yang telah diidentifikasi, dampaknya, serta strategi mitigasinya:

| # | Risiko | Dampak | Mitigasi | Terkait |
|---|--------|--------|----------|---------|
| R-01 | **Out Of Memory (OOM) pada Foto 12MP+** | Aplikasi crash saat melakukan scroll grid | Dekode thumbnail dibatasi pada `256px` di Coil `ImageLoader` + Paging 3 *chunking* 60 item | ADR-002, ADR-003 |
| R-02 | **Inkonsistensi Penghapusan Scoped Storage** | Error `SecurityException` saat mencoba menghapus file publik | Menggunakan `MediaStore.createDeleteRequest()` di API 30+ dan `RecoverableSecurityException` di API 29 | ADR-004 |
| R-03 | **Kebocoran Foto Vault ke MediaStore** | Foto rahasia tetap muncul di galeri publik / app lain | Berkas dipindahkan ke privat `context.filesDir/vault/` dan entri asli dihapus dari `MediaStore` | ADR-005 |
| R-04 | **Kebocoran Memori Bitmap di Editor** | Memori RAM membengkak saat penyesuaian filter real-time | Menggunakan `ColorMatrix` langsung pada rendering Canvas Compose tanpa mengalokasikan Bitmap baru berulang kali | ADR-006 |
| R-05 | **Kerusakan Berkas Foto Asli** | Foto asli pengguna hilang/tertutup setelah pengeditan | Operasi simpan wajib menulis sebagai berkas baru via `MediaStore.insert()` + `OutputStream` | ADR-006 |
| R-06 | **UI Freeze saat Komputasi pHash** | Frame drop / lag UI saat deteksi duplikat foto banyak | Perhitungan DCT pHash diwajibkan berjalan di background thread (`Dispatchers.IO`) via `PHashIndexingWorker` | ADR-011 |
| R-07 | **Race Condition Autentikasi Biometrik Vault** | Potensi UI state mismatch / timing error saat biometric unlock | Callback `BiometricPrompt` wajib dibungkus `viewModelScope.launch` sebelum mutasi `_uiState` | ADR-013 |
| R-08 | **Pengindeksan Foto Trashed & Memory Leak Bitmap** | Smart Album memuat foto terbuang; OOM jika pHash error | Filter `IS_TRASHED = 0` di `IndexingWorker` + `try-finally` recycle bitmap di `PHashIndexingWorker` | ADR-013 |
| R-09 | **Query Overhead MediaStore Media.Files** | Query melambat saat memuat gabungan foto dan video | Gunakan `MediaStore.Files.getContentUri("external")` dengan proyeksi kolom terindeks minimal | ADR-014 |
| R-10 | **Editor Bitmap Export Memory Pressure** | OOM crash saat ekspor foto editan resolusi tinggi (4K/8K) | Pustaka `SaveEditedPhotoUseCase` melakukan kompresi berkas langsung via Stream di `Dispatchers.IO` | ADR-016 |
| R-11 | **Dynamic Accent Color Recomposition Scope** | Re-composition berlebih pada seluruh layar Compose | Bungkus `GalleryTheme` dengan `DynamicColorScheme` yang dibatasi pada level akar `MainActivity` | ADR-015 |

---

## 5. Change Guide

Panduan langkah kerja bagi pengembang untuk melakukan kompilasi dan pengujian proyek:

### 5.1 Perintah Kompilasi & Pengujian Gradle

| Perintah Gradle | Kegunaan |
|---|---|
| `./gradlew assembleDebug` | Membangun berkas APK Debug untuk pengujian lokal |
| `./gradlew test` | Jalankan seluruh unit test pada Repository dan PagingSource |
| `./gradlew assembleRelease` | Membangun berkas APK Release final siap pakai |
| `./gradlew lint` | Validasi aturan linting Kotlin dan Android Lint |

---

*Dokumen ini dibuat berdasarkan analisis kode aktual pada 29 Juli 2026 | v1.14.2 (versionCode 45) | Commit: 1ee6df3*

