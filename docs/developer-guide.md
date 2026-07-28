# Developer Guide — Gallery App Android

> **Versi:** v1.7.0-M7 — 26 Juli 2026
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
│   │   │   ├── data/                         ← Lapisan Data (MediaStore & Vault Storage)
│   │   │   │   ├── local/                    ← Encrypted DataStore & File Vault Manager (Planned M8)
│   │   │   │   │   ├── VaultStorageManager.kt
│   │   │   │   │   └── SecurityPreferences.kt
│   │   │   │   ├── paging/                   ← Jetpack Paging 3 Source
│   │   │   │   │   └── MediaPagingSource.kt  [✅ M2]
│   │   │   │   └── repository/               ← Implementasi Repository
│   │   │   │       └── MediaRepositoryImpl.kt [✅ M2-M7]
│   │   │   │
│   │   │   ├── domain/                       ← Lapisan Domain (Model & Interface)
│   │   │   │   ├── model/                    ← Data Class
│   │   │   │   │   ├── PhotoItem.kt          [✅ M2]
│   │   │   │   │   ├── Album.kt              [✅ M4]
│   │   │   │   │   └── VaultItem.kt          (Planned M8)
│   │   │   │   ├── repository/               ← Contracts
│   │   │   │   │   └── MediaRepository.kt    [✅ M2-M7]
│   │   │   │   └── usecase/                  ← Logika Bisnis
│   │   │   │       ├── SaveEditedImageUseCase.kt (Planned M6)
│   │   │   │       └── MoveToVaultUseCase.kt (Planned M8)
│   │   │   │
│   │   │   ├── ui/                           ← Lapisan Antarmuka Jetpack Compose
│   │   │   │   ├── components/               ← Komponen UI Reusable
│   │   │   │   │   └── SelectionTopAppBar.kt [✅ M5]
│   │   │   │   ├── gallery/                  ← Layar Utama Galeri Grid
│   │   │   │   │   ├── GalleryGridScreen.kt  [✅ M2, M5]
│   │   │   │   │   └── GalleryViewModel.kt   [✅ M2, M5]
│   │   │   │   ├── viewer/                   ← Peninjau Foto Screen Penuh & Zoom
│   │   │   │   │   ├── MediaViewerScreen.kt  [✅ M3]
│   │   │   │   │   ├── PhotoInfoBottomSheet.kt [✅ M3]
│   │   │   │   │   ├── ViewerViewModel.kt    [✅ M3]
│   │   │   │   │   └── ZoomableImage.kt      [✅ M3]
│   │   │   │   ├── album/                    ← Pengelompokan & Grid Album
│   │   │   │   │   ├── AlbumListScreen.kt    [✅ M4]
│   │   │   │   │   ├── AlbumDetailScreen.kt  [✅ M4]
│   │   │   │   │   └── AlbumViewModel.kt     [✅ M4]
│   │   │   │   ├── permission/               ← Layar Penanganan Izin Media
│   │   │   │   │   ├── PermissionScreen.kt   [✅ M1]
│   │   │   │   │   └── PermissionState.kt    [✅ M1]
│   │   │   │   ├── editor/                   ← Layar Crop & Filter Foto
│   │   │   │   │   ├── ImageEditorScreen.kt  [✅ M6]
│   │   │   │   │   └── EditorViewModel.kt     [✅ M6]
│   │   │   │   ├── trash/                    ← Layar Sampah Sistem
│   │   │   │   │   ├── TrashScreen.kt        [✅ M7]
│   │   │   │   │   └── TrashViewModel.kt     [✅ M7]
│   │   │   │   ├── vault/                    ← Layar Bilik Rahasia & Biometrik (Planned M8)
│   │   │   │   └── theme/                    ← Material 3 Theme, Color, Type & Shape [✅ M1]
│   │   │   │
│   │   │   ├── di/                           ← Module Hilt Dependency Injection
│   │   │   │   ├── AppModule.kt              [✅ M1]
│   │   │   │   ├── CoilModule.kt             [✅ M2]
│   │   │   │   └── RepositoryModule.kt       [✅ M2]
│   │   │   │
│   │   │   ├── GalleryApplication.kt         [✅ M1]
│   │   │   └── MainActivity.kt               ← Single Activity & NavHost Setup [✅ M1-M7]
│   │   │
│   │   └── AndroidManifest.xml               ← Deklarasi Permissions & Main Activity
│   │
│   └── src/test/java/com/gallery/app/         ← Pengujian Unit (Unit Tests)
│       └── data/paging/
│           └── MediaPagingSourceTest.kt      [✅ M2]
│
├── gradle/
│   └── libs.versions.toml                    ← Version Catalog (Centralized dependency versions)
│
├── docs/                                     ← Dokumentasi Resmi Proyek
│   ├── architecture-decisions.md             ← ADR-001 s/d ADR-007
│   ├── features-and-tech-stack.md            ← Fitur MVP & Tech Stack lengkap
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

*Dokumen ini dibuat berdasarkan analisis kode aktual pada 26 Juli 2026.*
