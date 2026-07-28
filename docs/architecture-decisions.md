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

*Terakhir diperbarui: 28 Juli 2026 | v1.1.0 (versionCode 6)*
