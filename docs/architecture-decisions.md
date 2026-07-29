# Architecture Decision Records (ADR)
# Gallery App Android

> Dokumen ini mencatat keputusan arsitektur penting yang telah dibuat dan yang masih perlu diputuskan dalam pengembangan **Gallery App Android**.
> Setiap ADR mendokumentasikan: konteks, keputusan yang diambil, alasan, dan konsekuensi.

---

## Daftar ADR

| ID | Judul | Status | Tanggal |
|----|-------|--------|---------|
| [ADR-001](#adr-001) | Arsitektur UI: Single-Activity Jetpack Compose (Tanpa XML Layouts) | ✅ Diputuskan | 2026-07-26 |
| [ADR-002](#adr-002) | Large Media Listing: Jetpack Paging 3 untuk MediaStore Query | ✅ Diputuskan | 2026-07-26 |
| [ADR-003](#adr-003) | Image Loading & Memory Strategy: Tuning Coil 3 Cache & 256px Thumbnail | ✅ Diputuskan | 2026-07-26 |
| [ADR-004](#adr-004) | Penghapusan Media: Integrasi System Trash (`IS_TRASHED`) Scoped Storage | ✅ Diputuskan | 2026-07-26 |
| [ADR-005](#adr-005) | Isolation & Vault Security: Private App Storage + Hardware BiometricPrompt | ✅ Diputuskan | 2026-07-26 |
| [ADR-006](#adr-006) | Lightweight Image Editor: Compose Canvas Crop & ColorMatrix Filter | ✅ Diputuskan | 2026-07-26 |
| [ADR-007](#adr-007) | Dependency Injection & Version Management: Hilt DI + Gradle Version Catalog | ✅ Diputuskan | 2026-07-26 |
| [ADR-008](#adr-008) | Performance Optimization & Vault Cache Isolation Strategy | ✅ Diputuskan | 2026-07-28 |
| [ADR-009](#adr-009) | Local Metadata Caching (Room Database) & Background Indexing (WorkManager) | ✅ Diputuskan | 2026-07-28 |
| [ADR-010](#adr-010) | Multimedia Hub & Embedded Video Player: Jetpack Media3 (ExoPlayer) | ✅ Diputuskan | 2026-07-28 |
| [ADR-011](#adr-011) | Smart Organization: Custom DCT pHash, One-tap Bulk Cleanup, & Smart Albums | ✅ Diputuskan | 2026-07-28 |
| [ADR-012](#adr-012) | Dynamic MediaStore Sorting & 1-Click Selection State Strategy | ✅ Diputuskan | 2026-07-28 |
| [ADR-013](#adr-013) | Codebase Hardening, Dependency Injection Dispatchers, & Vault Security Enhancement | ✅ Diputuskan | 2026-07-28 |
| [ADR-014](#adr-014) | Universal Media Type Filtering Strategy via Floating Dock Control | ✅ Diputuskan | 2026-07-29 |
| [ADR-015](#adr-015) | DataStore Preferences Architecture for App Settings & Dynamic M3 Accent Colors | ✅ Diputuskan | 2026-07-29 |
| [ADR-016](#adr-016) | Comprehensive Photo Editor Engine (Canvas Live Preview, Adjustments, Presets & Export) | ✅ Diputuskan | 2026-07-29 |
| [ADR-017](#adr-017) | Android 15 Edge-to-Edge Compatibility & Target SDK 35 Upgrade | ✅ Diputuskan | 2026-07-29 |

---


## ADR-001
## Arsitektur UI: Single-Activity Jetpack Compose (Tanpa XML Layouts)

**Status**: ✅ Diputuskan

**Konteks**:
Pengembangan antarmuka pengguna Android modern memiliki dua opsi: arsitektur tradisional berbasis `View` + XML Layouts atau kerangka kerja deklaratif modern `Jetpack Compose`.

**Keputusan**:
Menggunakan **100% Jetpack Compose** dengan pola *Single-Activity Architecture* (`MainActivity.kt`). Seluruh komponen UI, navigasi, dan dialog dibuat secara deklaratif tanpa berkas XML layout.

**Alasan**:
- *Codebase* jauh lebih ringkas dan mudah dikembangkan oleh *developer* maupun perkakas AI.
- Integrasi animasi state UI yang sangat responsif (seperti gesture pinch-to-zoom dan penyesuaian filter real-time).
- Menghindari overheadinflating XML View yang memperlambat performa render layar.

**Konsekuensi**:
- Menggunakan `navigation-compose` untuk mengatur alur perpindahan antar layar (Gallery Grid, Viewer, Editor, Trash, Vault).
- Memerlukan pembatasan komposisi (*recomposition optimization*) untuk menjaga performa grid thumbnail tetap di 120fps.

---

## ADR-002
## Large Media Listing: Jetpack Paging 3 untuk MediaStore Query

**Status**: ✅ Diputuskan

**Konteks**:
Perangkat pengguna dapat menyimpan puluhan ribu foto. Mengambil (*query*) seluruh data foto dari `ContentResolver MediaStore` secara sekaligus ke memori akan menyebabkan *Out Of Memory (OOM)* crash pada aplikasi.

**Keputusan**:
Menggunakan **Jetpack Paging 3** (`androidx.paging:paging-compose`) dengan implementasi `PagingSource` kustom yang membaca data `MediaStore.Images.Media` secara bertahap (*chunking* 60 item per halaman).

**Alasan**:
- Hanya memuat foto yang saat ini terlihat (*viewport*) di layar pengguna.
- Manajemen memori otomatis saat pengguna melakukan gulir (*scrolling*) cepat pada grid foto.
- Terintegrasi secara bawaan dengan `StateFlow` dan Coroutines.

**Konsekuensi**:
- Kueri `MediaStore` harus selalu dieksekusi di `Dispatchers.IO`.
- Repository mempublikasikan data sebagai `Flow<PagingData<PhotoItem>>`.

---

## ADR-003
## Image Loading & Memory Strategy: Tuning Coil 3 Cache & 256px Thumbnail

**Status**: ✅ Diputuskan

**Konteks**:
Mendekode gambar beresolusi tinggi (misal: 12 MP s/d 48 MP) secara langsung pada grid galeri dapat menghabiskan RAM perangkat dalam hitungan detik.

**Keputusan**:
Menerapkan konfigurasi `ImageLoader` kustom pada **Coil 3**:
- Minta ukuran thumbnail secara eksplisit (`size(256)`).
- Menyiapkan batas `MemoryCache` dan `DiskCache` yang terukur pada `ImageLoader`.
- Hanya memuat resolusi asli (*full resolution*) saat pengguna membuka tampilan `HorizontalPager` Viewer atau Editor.

**Alasan**:
- Mencegah kehabisan memori (*OOM*) dan menjamin *scrolling* tetap lancar pada layar dengan *refresh rate* tinggi (90Hz / 120Hz).
- Menghemat konsumsi daya baterai dan penggunaan memori RAM perangkat.

**Konsekuensi**:
- Gambar grid terlihat sedikit lebih rendah resolusinya dibanding viewer penuh (dapat diterima demi performa).

---

## ADR-004
## Penghapusan Media: Integrasi System Trash (`IS_TRASHED`) Scoped Storage

**Status**: ✅ Diputuskan

**Konteks**:
Sejak Android 10+ (API 29+), Scoped Storage membatasi akses hapus langsung ke berkas media publik. Aplikasi dapat membuat folder tempat sampah buatan sendiri atau memanfaatkan *System Trash* bawaan MediaStore.

**Keputusan**:
Menggunakan **System Trash bawaan MediaStore** berbasis kolom `IS_TRASHED` (API 30+):
- Aksi hapus biasa memicu `MediaStore.createTrashRequest(uris, trash = true)`.
- Layar utama galeri selalu memfilter `IS_TRASHED = 0`.
- Layar Trash memuat media dengan filter `IS_TRASHED = 1`.
- API 29 menggunakan penanganan `RecoverableSecurityException`, sedangkan API < 29 menghapus berkas secara langsung dengan izin penulisan.

**Alasan**:
- Mematuhi aturan standar Android Scoped Storage tanpa perlu izin pemindaian berkas privat yang berlebihan.
- Berkas di-purge otomatis oleh sistem Android setelah 30 hari tanpa perlu timer background sendiri.

**Konsekuensi**:
- Proses hapus dan pemulihan memicu dialog konfirmasi resmi dari OS Android untuk keamanan pengguna.

---

## ADR-005
## Isolation & Vault Security: Private App Storage + Hardware BiometricPrompt

**Status**: ✅ Diputuskan

**Konteks**:
Fitur *Hidden Vault* membutuhkan jaminan bahwa foto yang disembunyikan tidak dapat diindeks oleh aplikasi galeri lain maupun diakses tanpa autentikasi pengguna.

**Keputusan**:
1. **Isolasi Berkas**: Memindahkan berkas foto dari media publik ke direktori privat internal aplikasi (`context.filesDir/vault/`), lalu menghapus salinan publik asli dari `MediaStore`.
2. **Kemanan Akses**: Membuka vault wajib melewati autentikasi **`BiometricPrompt`** (sidik jari/wajah) dengan fallback PIN terenkripsi hardware via **Encrypted DataStore** (`androidx.security:security-crypto`).
3. **PemberSIhan Memori**: Thumbnail vault hanya didekode secara sementara di RAM selama sesi vault terbuka.

**Alasan**:
- File di `filesDir` secara otomatis terisolasi oleh sandbox OS Android dan tidak dapat di-scan oleh MediaStore maupun aplikasi pihak ketiga.
- Keamanan hardware-level mencegah pembobolan PIN dari penyimpanan berkas preferensi.

**Konsekuensi**:
- Mengembalikan foto dari vault (*Restore*) mewajibkan penulisan ulang berkas ke MediaStore via `insert()`.

---

## ADR-006
## Lightweight Image Editor: Compose Canvas Crop & ColorMatrix Filter

**Status**: ✅ Diputuskan

**Konteks**:
Fitur pengeditan foto membutuhkan pemotongan (*crop*) dan filter warna. Menggunakan library eksternal yang berat atau RenderScript (yang sudah *deprecated*) akan menambah ukuran APK dan risiko bentrok dependensi.

**Keputusan**:
Membangun editor kustom ringan berbasis **Compose Canvas** dan **`ColorMatrix`**:
- **Crop UI**: Gestur seret (*drag*) dan *zoom* pada Canvas Compose custom dengan pilihan rasio Bebas, 1:1, atau 4:3.
- **Filter**: Menggunakan `ColorFilter.colorMatrix(ColorMatrix(...))` untuk efek Grayscale, Sepia, Warm, Cool, dan Contrast.
- **Simpan**: Hasil pengeditan didekode dengan `inSampleSize` yang aman dan disimpan sebagai **file baru** via `MediaStore.insert()`.

**Alasan**:
- Tanpa dependensi berat tambahan, ukuran APK tetap kecil.
- Menjamin foto asli pengguna tidak pernah tertimpa atau rusak.

**Konsekuensi**:
- Pengeditan berbasis Canvas memerlukan kalkulasi matriks transformasi bitmap yang presisi.

---

## ADR-007
## Dependency Injection & Version Management: Hilt DI + Gradle Version Catalog

**Status**: ✅ Diputuskan

**Konteks**:
Manajemen dependensi dan pemisahan modul pada aplikasi Android memerlukan arsitektur penyedia dependensi yang terisolasi serta terpusat.

**Keputusan**:
- Menggunakan **Google Hilt** (`@HiltAndroidApp`, `@Inject`, `@HiltViewModel`) untuk Injeksi Dependensi.
- Menggunakan **Gradle Version Catalog** (`gradle/libs.versions.toml`) untuk mengelola seluruh versi dependensi pustaka.

**Alasan**:
- Hilt terintegrasi sempurna dengan Compose dan siklus hidup ViewModel.
- Version Catalog memastikan tidak ada bentrok versi pustaka antar modul proyek.

---

## ADR-008
## Performance Optimization & Vault Cache Isolation Strategy

**Status**: ✅ Diputuskan

**Konteks**:
Scrolling grid galeri mengalami penurunan frame rate (jank) karena tidak tersedianya pembatasan cache memori eksplisit, durasi animasi crossfade yang terlalu lama pada thumbnail, dan pengumpulan Flow yang tidak *lifecycle-aware*. Selain itu, pemuatan berkas vault menggunakan `ImageLoader` global berisiko menyisakan berkas thumbnail terdekode di *disk/memory cache* publik.

**Keputusan**:
1. **Global ImageLoader Singleton**: Dikonfigurasi di `GalleryApplication` dengan `MemoryCache` max 30% heap limit dan `DiskCache` max 5% disk limit.
2. **Vault Cache Isolation**: Memuat thumbnail `VaultScreen` menggunakan `ImageLoader` terisolasi tanpa memory cache dan disk cache (`memoryCache(null)`, `diskCache(null)`).
3. **Lifecycle-Aware State Collection**: Seluruh Composable screen migrasi ke `collectAsStateWithLifecycle()` dari pustaka `androidx.lifecycle:lifecycle-runtime-compose`.
4. **PagingConfig Tuning**: Menyesuaikan `pageSize = 30`, `initialLoadSize = 60`, `prefetchDistance = 15`, dan `maxSize = 200` untuk membatasi ukuran heap memori.

**Alasan**:
- Menjamin scroll grid berjalan di 60fps/120fps tanpa risiko OOM crash.
- Menjamin keamanan dan privasi berkas vault dari potensi kebocoran cache ke direktori publik.
- Mencegah pengolahan data dan rekomposisi UI yang tidak perlu saat aplikasi berada di background.

**Konsekuensi**:
- Seluruh screen Composable wajib menggunakan `collectAsStateWithLifecycle()`.

---

## ADR-009
## Local Metadata Caching (Room Database) & Background Indexing (WorkManager)

**Status**: ✅ Diputuskan

**Konteks**:
Mengueri `ContentResolver` MediaStore berulang kali pada setiap pembukaan layar galeri atau penyaringan data tidak dapat diskalakan secara efisien untuk perangkat yang memiliki puluhan ribu berkas media. Selain itu, fitur lanjutan Fase P2 (Duplicate Photo Finder & Smart Albums) membutuhkan pencarian dan perbandingan metadata yang cepat secara offline tanpa membebankan thread utama.

**Keputusan**:
Menerapkan strategi **Hybrid Cache-First (Opsi A)**:
1. **Room Database**: Menyiapkan `GalleryDatabase`, `CachedPhotoEntity` (termasuk kolom EXIF GPS `latitude`, `longitude`), dan `PhotoDao` untuk pengindeksan metadata lokal.
2. **WorkManager Background Indexing**: Menjalankan `IndexingWorker` (`CoroutineWorker`) di `Dispatchers.IO` dengan constraint `setRequiresBatteryNotLow(true)` untuk mengesktrak metadata dari `MediaStore` dan menyimpannya ke Room DB tanpa memblokir thread UI.
3. **Reaktif ContentObserver**: Menggunakan `MediaStoreObserver` untuk mendeteksi perubahan berkas media secara real-time dan memicu pemutakhiran cache inkremental otomatis (debounced 3 detik).
4. **Settings Hatch**: Menyiapkan seksi kartu pada `SettingsScreen` yang menyajikan status jumlah foto ter-index di Room DB dan tombol manual trigger `Sinkronkan Ulang Index`.

**Alasan**:
- Menghilangkan *delay* initial load saat aplikasi dibuka dan menyediakan pencarian metadata super cepat.
- UI grid galeri utama tetap real-time (`MediaPagingSource`) tanpa risiko *stale data*, sementara Room DB siap digunakan sebagai fondasi pengolahan AI/ML di Fase P2.
- Operasi I/O dieksekusi terisolasi pada background thread tanpa menyebabkan *Application Not Responding (ANR)*.

**Konsekuensi**:
- Memerlukan tambahan pustaka `androidx.room` (v2.6.1) dan `androidx.work` (v2.9.0) di `libs.versions.toml`.
- Schema Room menyertakan bidang GPS sejak awal untuk menghindari migrasi basis data berulang di Fase P2.

---

## ADR-010
## Multimedia Hub & Embedded Video Player: Jetpack Media3 (ExoPlayer)

**Status**: ✅ Diputuskan

**Konteks**:
Pengguna membutuhkan pemutaran berkas video secara internal di dalam aplikasi tanpa harus dialihkan ke aplikasi eksternal (*Intent launcher*).

**Keputusan**:
1. Menggunakan **Jetpack Media3 (v1.3.1)** (`media3-exoplayer`, `media3-ui`, `media3-common`).
2. Membuat `VideoPlayerScreen` berbasis `AndroidView` + `PlayerView` yang dikendalikan oleh `VideoPlayerViewModel`.
3. Menerapkan pelepasan memori otomatis `player.release()` menggunakan `DisposableEffect` saat pengguna keluar dari layar pemutar video.

**Alasan**:
- Media3 adalah standar resmi Google untuk multimedia di Android dengan pemutaran video yang sangat stabil dan hemat daya.
- Mencegah kebocoran memori (*memory leak*) pada ExoPlayer `Player` instance.

**Konsekuensi**:
- Penambahan 3 library `androidx.media3` di `libs.versions.toml` dan `app/build.gradle.kts`.

---

## ADR-011
## Smart Organization: Custom DCT pHash, One-tap Bulk Cleanup, & Smart Albums

**Status**: ✅ Diputuskan

**Konteks**:
Pembersihan galeri dari berkas duplikat dan pengelompokan otomatis album pintar membutuhkan pengolahan metadata dan algoritma kemiripan gambar (*perceptual hashing*).

**Keputusan**:
1. **Custom pHash Calculator**: Mengimplementasikan algoritma Discrete Cosine Transform (DCT) 64-bit berbasis Android `Bitmap` API (`PHashCalculator.kt`) dengan perbandingan Hamming distance (threshold ≤ 10). Zero external dependency.
2. **Room Migration (v1 → v2)**: Menambahkan kolom `pHash TEXT` dan index `index_cached_photos_pHash` di `CachedPhotoEntity` dengan `MIGRATION_1_2`.
3. **One-tap Bulk Cleanup**: Tombol "✨ Bersihkan Semua" di `DuplicateScreen` yang mempertahankan 1 berkas berukuran terbesar (kualitas terbaik) per grup dan mengirim sisa duplikat ke System Trash via `createTrashRequest`.
4. **Smart Albums**: Pengelompokan dinamis (Video, Screenshot, Geotagged, Selfie) langsung dari kueri Room DB tanpa bergantung pada pustaka ExifInterface.

**Alasan**:
- Mengurangi dependensi eksternal (APK size hemat).
- Pengolahan pHash berjalan aman di background thread via `PHashIndexingWorker`.
- Keamanan Scoped Storage tetap terjaga (selalu menggunakan `createTrashRequest`).

---

## ADR-012
## Dynamic MediaStore Sorting & 1-Click Selection State Strategy

**Status**: ✅ Diputuskan

**Konteks**:
Pengguna membutuhkan pengurutan foto secara dinamis (Terbaru, Terlama, Nama A-Z/Z-A, Ukuran) serta mekanisme seleksi foto massal 1-klik tanpa dipaksa melakukan gesture *long-press*.

**Keputusan**:
1. **Dynamic MediaStore Query**: Menggunakan enum `SortOption` yang diterjemahkan ke dalam parameter `ContentResolver.QUERY_ARG_SORT_COLUMNS` dan `ContentResolver.QUERY_ARG_SORT_DIRECTION`.
2. **Reactive Paging Invalidation**: ViewModel mengelola `_sortOption: StateFlow<SortOption>` dan menggunakan `flatMapLatest` untuk meng-invalidate `MediaPagingSource` secara otomatis saat kriteria sorting berubah.
3. **1-Click Selection Entry & Select All**:
   - Menambahkan ikon *Checklist* di TopBar untuk langsung masuk ke `isSelectionMode = true`.
   - Menambahkan ikon *DoneAll* di `FloatingDockActionBar` untuk beralih antara memilih seluruh item yang sedang dimuat (*loaded items*) atau mengosongkan centang pilihan (*Deselect All*).

**Alasan**:
- Mencegah kebutuhan kueri ulang manual dan menjaga Paging 3 tetap reaktif.
- Meningkatkan efisiensi navigasi dan UX saat melakukan tindakan massal (Share, Hide to Vault, Delete).

**Konsekuensi**:
- UI grid mengintegrasikan `SortBottomSheet` Material 3 untuk pemilih opsi sorting.

---

## ADR-013
## Codebase Hardening, Dependency Injection Dispatchers, & Vault Security Enhancement

**Status**: ✅ Diputuskan

**Konteks**:
Hasil audit E2E mengidentifikasi beberapa titik risiko: penggunaan `Dispatchers.IO` hardcoded tanpa qualifier Hilt, callback BiometricPrompt di Main Thread tanpa coroutine scope (race condition), bitmap pHash yang tidak di-recycle jika exception, pengindeksan foto trashed di `IndexingWorker`, callback lambda dari ViewModel ke UI, dan ketiadaan batas percobaan PIN Vault.

**Keputusan**:
1. **Hilt `@IoDispatcher` Qualifier**: Dibuat di `DispatcherModule.kt` dan diinjeksikan ke seluruh Repository & UseCases.
2. **Vault Cache & Biometric Scope**: Menginjeksi `@VaultImageLoader` via Hilt ke `VaultViewModel` dan membungkus callback BiometricPrompt ke `viewModelScope.launch`.
3. **Worker Safety**: Mengisi `selection = IS_TRASHED = 0` di `IndexingWorker` dan menyelimuti pHash bitmap decode dengan `try-finally` recycle di `PHashIndexingWorker`.
4. **PIN Lockout**: Terapkan batas 5x kegagalan PIN dengan 30-detik lockout backoff di `PinEncryptionHelper`.
5. **Decoupled UI Events**: `TrashViewModel` beralih ke `SharedFlow<TrashUiEvent>` pattern.

**Alasan**:
- Menjamin stabilitas memori, isolasi cache privat vault, keamanan PIN dari brute-force attack, dan pengujian unit yang 100% konsisten.

**Konsekuensi**:
- Seluruh UseCase dan Repository wajib menerima `@IoDispatcher CoroutineDispatcher` via konstruktor.

---

## ADR-014
## Universal Media Type Filtering Strategy via Floating Dock Control

**Status**: ✅ Diputuskan

**Konteks**:
Pengguna memerlukan opsi untuk dengan mudah memfilter tampilan berkas media (Semua Media, Foto Saja, Video Saja) secara konsisten dan cepat tanpa membingungkan navigasi.

**Keputusan**:
1. Mengubah tombol pertama pada `FloatingDock` dari tombol statis "Foto" menjadi **Media Type Filter Control** (`ALL`, `PHOTOS_ONLY`, `VIDEOS_ONLY`).
2. Menggunakan kueri unified `MediaStore.Files.getContentUri("external")` di `MediaPagingSource` untuk mendukung pemuatan gabungan media foto (`MEDIA_TYPE_IMAGE`) dan video (`MEDIA_TYPE_VIDEO`).
3. Menerapkan state penyaringan ini secara universal di Galeri Utama (`GalleryHomeScreen`), Detail Album (`AlbumDetailScreen`), dan Hidden Vault (`VaultScreen`).

**Alasan**:
- Antarmuka terasa mulus dan modern tanpa membutuhkan dropdown atau tab terpisah yang memakan area layar.
- Performa Paging 3 tetap optimal karena filter diterapkan langsung pada tingkat kueri `ContentResolver` MediaStore.

**Konsekuensi**:
- Parameter filter `mediaTypeFilter` diinjeksi ke `MediaPagingSource` dan dikelola via `StateFlow` di ViewModel.

---

## ADR-015
## DataStore Preferences Architecture for App Settings & Dynamic M3 Accent Colors

**Status**: ✅ Diputuskan

**Konteks**:
Fitur Pengaturan Aplikasi (Settings) memerlukan penyimpanan preferensi pengguna (seperti tema, warna aksen Material 3, default filter startup, auto-play video, mute audio, dan lock delay Vault) yang aman, reaktif, dan terintegrasi dengan Hilt.

**Keputusan**:
1. Menggunakan **Jetpack DataStore Preferences** (`androidx.datastore:datastore-preferences`) via `ThemeRepositoryImpl` dan `SettingsPreferences`.
2. Menyediakan 4 pilihan warna aksen Material 3 (Emerald Green, Ocean Blue, Sunset Orange, Royal Purple) yang mengubah `ColorScheme` Compose secara reaktif.
3. Menyediakan fungsi Cache Cleaner 1-klik untuk menghapus cache memori/disk Coil tanpa mengganggu berkas media asli.

**Alasan**:
- DataStore menggantikan SharedPreferences usang dengan eksekusi Coroutines I/O yang *asynchronous* dan *type-safe*.
- Mengubah warna aksen aplikasi secara instan tanpa memerlukan restart aplikasi.

**Konsekuensi**:
- Lapisan UI dibungkus oleh `GalleryTheme` yang mengamati `ThemeRepository.getAccentColor()` via `StateFlow`.

---

## ADR-016
## Comprehensive Photo Editor Engine (Canvas Live Preview, Adjustments, Presets & Export)

**Status**: ✅ Diputuskan

**Konteks**:
Pengguna membutuhkan pengeditan foto yang kaya fitur (potong gambar, penyesuaian kecerahan/kontras/saturasi/warmth/vignette, rotasi/flip, 10+ preset filter visual, coretan lukis brush, dan teks) serta opsi memilih format & kualitas ekspor.

**Keputusan**:
1. Membangun engine editor berbasis **Compose Canvas & `graphicsLayer`** untuk *live preview* real-time tanpa alokasi memori berlebih.
2. Menyediakan tab kontrol: Crop, Adjustments, Rotate/Flip, Presets, Doodle/Text.
3. Ekspor foto dilakukan via `SaveEditedPhotoUseCase` yang menghasilkan berkas baru di `MediaStore` dengan pilihan format (`JPEG`, `PNG`, `WEBP`) dan slider kompresi kualitas (`50%` s/d `100%`).
4. Berkas hasil edit disimpan di folder asal yang sama (`RELATIVE_PATH`) dengan media yang diedit.

**Alasan**:
- Menjamin imutabilitas foto asli pengguna.
- Performa live preview sangat responsif tanpa membebankan RAM (bebas OOM).

**Konsekuensi**:
- Proses ekspor menulis berkas secara asinkron di `Dispatchers.IO`.

---

## ADR-017
## Android 15 Edge-to-Edge Compatibility & Target SDK 35 Upgrade

**Status**: ✅ Diputuskan

**Konteks**:
Mulai Android 15 (API 35), aplikasi diwajibkan mendukung tampilan *edge-to-edge* secara penuh di mana status bar dan navigation bar transparan.

**Keputusan**:
1. Meng-upgrade `compileSdk = 35` dan `targetSdk = 35` pada `app/build.gradle.kts`.
2. Memanggil `enableEdgeToEdge()` di `MainActivity.kt` sebelum `setContent {}`.
3. Menyesuaikan insets padding pada Jetpack Compose `Scaffold` dan `Surface`.

**Alasan**:
- Memenuhi standar kepatuhan aplikasi Android rilis terbaru di Google Play Store dan Android 15.

**Konsekuensi**:
- Memastikan elemen Floating Dock dan TopBar tidak tertutup oleh insets sistem perangkat.

---

*Terakhir diperbarui: 29 Juli 2026 | v1.14.2 (versionCode 45)*

