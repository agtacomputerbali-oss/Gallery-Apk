# Gallery App — Spesifikasi MVP

> Dokumen pengembangan aplikasi gallery Android
> IDE: Antigravity | Rev 2 — 26 Juli 2026 (perluasan scope: Editor, Trash, Hidden vault → P0)

---

## 1. Scope MVP

**Prinsip:** buka cepat, scroll mulus, lihat enak, kelola aman. Sisanya nanti.

### P0 — Wajib ada

- ✅ Load foto dari perangkat via `MediaStore` (bukan scan folder manual)
- ✅ Grid thumbnail + fast scroll (`LazyVerticalGrid`)
- ✅ Viewer full-screen: pinch-zoom + swipe antar foto (`HorizontalPager`)
- ✅ Pengelompokan album (bucket) dari `BUCKET_DISPLAY_NAME`
- ✅ Multi-select → share (`ACTION_SEND_MULTIPLE`) & delete
- ✅ Runtime permission Android 13+ (`READ_MEDIA_IMAGES`)
- ✅ Editor: crop + filter (hasil disimpan sebagai file baru)
- ✅ Trash / recycle bin (restore & hapus permanen)
- ✅ Hidden vault + app lock (biometric / PIN)

### Ditunda (bukan bagian MVP)

- Video player
- Search
- Cloud backup / sync

### Catatan teknis kritis

**Dasar:**
- **Delete** mengikuti Scoped Storage:
  - API 30+ → `MediaStore.createDeleteRequest()` (dialog konfirmasi sistem)
  - API 29 → tangkap `RecoverableSecurityException`, lanjutkan intent
  - < 29 → hapus langsung dengan permission write
- **Performa grid**: jangan decode ukuran penuh; minta thumbnail eksplisit ke Coil (`size(256)`), atur `MemoryCache` & `DiskCache` terukur pada `ImageLoader` untuk mencegah OOM.
- **Paging 3**: gunakan `androidx.paging:paging-compose` (`PagingSource` bertahap per 60 item) untuk query `MediaStore`, bukan memuat seluruh foto sekaligus ke memori.
- **Query** selalu di `Dispatchers.IO`, hasil di-expose sebagai `Flow<PagingData<PhotoItem>>`.

**Editor:**
- Crop: canvas Compose custom (gesture drag + zoom, rasio bebas / 1:1 / 4:3)
- Filter: preview via `ColorFilter` + `ColorMatrix` (grayscale, sepia, warm, cool, contrast) — hindari RenderScript (deprecated)
- Simpan hasil sebagai **file baru** via `MediaStore.insert()` + `OutputStream` — file asli tidak boleh berubah
- Decode bitmap dengan `inSampleSize` sesuai layar untuk hindari OOM

**Trash:**
- Gunakan **trash sistem**, bukan folder buatan sendiri: kolom `IS_TRASHED` (API 30+)
- File bukan milik app → `MediaStore.createTrashRequest(uris, trash = true/false)` (memicu dialog sistem)
- Grid utama selalu filter `IS_TRASHED = 0`; layar Trash query `IS_TRASHED = 1`
- Sistem auto-purge setelah 30 hari — tidak perlu timer sendiri
- Fallback API < 30: pindahkan file ke folder privat app, hapus dari MediaStore

**Hidden vault:**
- Foto dipindah ke **internal storage app** (tidak terlihat MediaStore), lalu original dihapus via `createDeleteRequest`
- Thumbnail vault di-decode hanya selama sesi vault terbuka; jangan di-cache ke direktori publik
- App lock: `androidx.biometric.BiometricPrompt` + fallback PIN terenkripsi via `androidx.security:security-crypto` + DataStore
- Restore = tulis ulang ke MediaStore via `insert()`, hapus salinan privat

---

## 2. Tech Stack

| Komponen | Pilihan | Alasan |
|---|---|---|
| Bahasa | Kotlin | Dukungan tooling & agent paling matang |
| UI | Jetpack Compose + Material 3 | Deklaratif, modern, performa animasi tinggi |
| Large List / Grid | Jetpack Paging 3 (`paging-compose`) | Pagination otomatis MediaStore untuk ribuan foto tanpa OOM |
| Image loading | Coil 3 (`coil-compose`) + Custom Cache | Ringan, Compose-native, memory & disk cache tuning (256px thumbnail) |
| Arsitektur | MVVM + Repository + Hilt DI | ViewModel + `StateFlow` / `PagingData`, Dependency Injection terisolasi |
| Async | Coroutines + Flow | Query `ContentResolver` aman di background |
| Editor | Compose Canvas + `ColorMatrix` | Tanpa dependensi berat, mudah diiterasi agent |
| Keamanan | BiometricPrompt + Security Crypto DataStore | App lock modern, PIN terenkripsi hardware-level |
| Min SDK | 26 (Android 8.0) | ~95%+ perangkat, menyederhanakan cabang kode |
| Build | Gradle KTS + Version Catalog (`libs.versions.toml`) | Manajemen versi dependensi terpusat & bebas bentrok |

---

## 3. Roadmap Milestone

Satu milestone = satu task agent di Antigravity. Jangan lanjut sebelum kriteria verifikasi hijau.

### M1 — Scaffold, Dependency Injection & Permission
- ✅ Setup project Compose + Material 3 + Hilt DI + Version Catalog (`libs.versions.toml`)
- ✅ Runtime permission `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (fallback)
- ✅ Empty state & denied state

**Verifikasi:** build hijau dengan Hilt; dialog permission muncul; grant → lanjut ke M2; deny → tampilan alasan + tombol ke Settings.

### M2 — Repository, Paging 3 & Grid
- ✅ `MediaPagingSource` & `MediaRepository.getImages(): Flow<PagingData<PhotoItem>>` via `MediaStore` (filter `IS_TRASHED = 0`)
- ✅ Grid thumbnail dengan Coil custom ImageLoader, loading spinner
- ✅ Unit test repository & PagingSource dengan cursor palsu

**Verifikasi:** foto perangkat tampil berhalaman (chunk 60); scroll 120fps; `./gradlew test` hijau.

### M3 — Viewer
- ✅ `HorizontalPager` antar foto
- ✅ Pinch-zoom + double-tap zoom
- ✅ Info dasar: nama file, tanggal, resolusi
- ✅ Tombol akses Editor dari viewer (placeholder hingga M6 selesai)

**Verifikasi:** zoom mulus tanpa OOM pada foto besar (>12 MP).

### M4 — Album
- ✅ `MediaRepository.getAlbums(): Flow<List<Album>>` (grouping per bucket)
- ✅ Layar daftar album: cover + jumlah foto
- ✅ Klik album → grid terfilter

**Verifikasi:** jumlah foto per album konsisten dengan total grid utama.

### M5 — Aksi: Share & Delete
- ✅ Mode multi-select (long-press)
- ✅ Share via `ACTION_SEND_MULTIPLE` + grant URI
- ✅ Delete dengan flow Scoped Storage (lihat catatan teknis)

**Verifikasi:** share ke WhatsApp/Drive berhasil; delete memicu dialog sistem dan file benar-benar hilang.

### M6 — Editor (Crop & Filter)
- ✅ UI crop: gesture drag/zoom, rasio bebas / 1:1 / 4:3
- ✅ 6–8 filter via `ColorMatrix` dengan preview real-time
- ✅ Simpan sebagai file baru via `MediaStore.insert()` (original utuh)

**Verifikasi:** hasil edit muncul di gallery sebagai file terpisah; original tidak berubah; tanpa OOM pada foto 12 MP+.

### M7 — Trash / Recycle Bin
- ✅ Aksi "hapus" memindahkan ke trash sistem (`IS_TRASHED = 1`, `createTrashRequest`)
- ✅ Layar Trash: daftar, restore (`trash = false`), hapus permanen (`createDeleteRequest`)
- ✅ Grid utama & album selalu memfilter `IS_TRASHED = 0`

**Verifikasi:** foto terhapus hilang dari grid, tampil di Trash; restore mengembalikannya ke grid; hapus permanen menghapus dari perangkat.

### M8 — Hidden Vault & App Lock
- ✅ Sembunyikan: salin ke internal storage app + hapus original dari MediaStore
- ✅ Layar vault dengan thumbnail (decode hanya selama sesi)
- ✅ `BiometricPrompt` + fallback PIN terenkripsi; preferensi di Encrypted DataStore
- ✅ Restore dari vault kembali ke MediaStore

**Verifikasi:** foto tersembunyi hilang dari gallery app & gallery sistem; vault hanya terbuka via biometric/PIN; restore berhasil.

### M9 — Polish & Rilis
- ✅ Ikon aplikasi custom (Adaptive Icon) + label "OP Gallery" + dark theme
- ✅ Signing config release & R8 minification / ProGuard rules
- ✅ `./gradlew assembleRelease` → APK final ter-sign siap rilis

**Verifikasi:** APK ter-install di perangkat fisik tanpa crash pada cold start; unit test & build `BUILD SUCCESSFUL`.

---

## 4. Aturan Proyek untuk Agent (opsional, taruh di rules Antigravity)

- Kotlin + Compose only, tanpa XML layout
- Pola MVVM + Hilt: `XxxScreen` (Compose) → `XxxViewModel` (Hilt) → `Repository`
- Semua akses `ContentResolver` hanya lewat repository dengan `PagingSource`
- Trash = `IS_TRASHED` sistem; dilarang membuat folder trash sendiri
- File vault hanya di internal storage app; dilarang menulis ke direktori publik
- Setiap task wajib diakhiri `./gradlew assembleDebug` hijau