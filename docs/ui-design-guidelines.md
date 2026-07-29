# Panduan Desain UI/UX & Design System — Gallery App Android

> **⭐ SINGLE SOURCE OF TRUTH untuk standar UI/UX antarmuka Jetpack Compose pada proyek Gallery App Android.**
> Dokumen ini wajib dipatuhi sebelum membuat atau merestrukturisasi komponen UI.

---

## 🎯 Identitas Proyek & Prinsip Desain

- **Produk**: Gallery App Android — Aplikasi Galeri Foto & Vault Media Modern
- **Target Pengguna**: Pengguna smartphone Android yang menginginkan galeri foto cepat, responsif, kaya fitur edit, dan aman.
- **Prinsip Utama**: *Content-First*, *Ultra-Fast Scrolling*, *Immersive Viewing*, dan *Privacy-Oriented*.
- **Estetika**: Material Design 3 (Material You) modern dengan tema gelap adaptif (*Dark Mode Default for Preview*) untuk memaksimalkan kontras tampilan foto.

---

## 🔤 Tipografi (Typography)

Menggunakan sistem **Material 3 Typography** untuk Jetpack Compose (`Typography.kt`):

| Peran | Style Name | Ukuran / Weight | Penggunaan |
|---|---|---|---|
| **Header Layar** | `titleLarge` | 22sp / SemiBold | Judul TopBar utama (misal: "Foto", "Album", "Vault") |
| **Subtitle / Subhead** | `titleMedium` | 16sp / Medium | Nama album, info detail metadata foto |
| **Body Utama** | `bodyMedium` | 14sp / Normal | Teks deskripsi, dialog konfirmasi |
| **Caption / Badge** | `labelSmall` | 11sp / Bold | Indikator jumlah media, counter multi-select |
| **Keypad PIN** | `displaySmall` | 32sp / Bold | Angka tombol PIN pada layar Vault Lock |

---

## 🎨 Palet Warna & Tema (Color System)

Seluruh warna didefinisikan menggunakan **Material 3 Color Scheme** (`Color.kt` & `Theme.kt`):

### Peran Warna Semantik (Semantic Roles)
- **Primary / Accent**: Warna aksen utama (misal: Deep Violet / Ocean Blue) untuk tombol CTA, item terpilih, dan *selection badge*.
- **Surface & Background**: Latar belakang aplikasi dengan gradasi gelap elegan (`#121212` / `#1E1E1E` di Dark Mode) agar warna foto terlihat menonjol.
- **Surface Variant**: Latar belakang kartu album, bar navigasi bawah (*Bottom Navigation Bar*), dan kontainer dialog.
- **Error / Destructive**: Merah terang semantik untuk tombol "Hapus Permanen" atau batas peringatan.

---

## 📐 Tata Letak Grid & Spacing System

### 1. Grid Thumbnail Utama (`GalleryGridScreen`)
- **Struktur Grid**: `LazyVerticalGrid` dengan `GridCells.Adaptive(minSize = 100.dp)` (menghasilkan 3–4 kolom dinamis sesuai resolusi layar).
- **Rasio Aspek Cell**: Persis **1:1 (Square Tile)**.
- **Spacing Antar Cell**: `2.dp` s/d `4.dp` untuk menghemat ruang dan menyajikan tampilan media yang rapat serta impresif.
- **Selection Badge**: Lingkaran ceklis warna Primary di sudut kanan atas thumbnail saat mode multi-select aktif.

### 2. Peninjau Layar Penuh (`MediaViewerScreen`)
- **Container**: `HorizontalPager` penuh layar (`100vw x 100vh`) dengan latar belakang hitam murni (`#000000`).
- **Interaksi Gestur**:
  - *Pinch-to-zoom*: Perbesaran skala foto hingga 3x-4x via `ZoomableImage.kt`.
  - *Double-tap zoom*: Pintasan cepat perbesaran 2x pada titik sentuh.
  - *Swipe horizontally*: Berpindah antar foto secara presisi.
- **TopBar & BottomBar Overlay**: Tampil/sembunyi secara otomatis (*toggle visibility*) saat layar di-tap sekali, dilengkapi *Metadata Bottom Sheet* (`PhotoInfoBottomSheet.kt`).

---

## 🎨 UI System Professional Image Editor (`EditorScreen`)

1. **Canvas Live Preview & Crop Overlay**:
   - Menampilkan kerangka garis potong (*crop handles*) di atas canvas foto dengan grid interaktif saat tab Crop aktif.
   - Mendukung penyesuaian rasio instan (Bebas, 1:1, 4:3, 16:9) via tombol selector.
2. **Tab Navigation Bar & Control Sliders**:
   - 5 Tab Utama: 📐 Crop & Ratio, 🎛️ Penyetelan (Brightness, Contrast, Saturation, Warmth, Vignette), 🔄 Rotasi/Flip (90° CCW/CW, Flip H/V), 🎨 Preset Filter (10+ thumbnail horizontal: Original, Vivid, Noir, Cyberpunk, dll), ✏️ Doodle & Teks (Color Picker, Stroke Width slider, Undo, Text input).
3. **Export Bottom Sheet**:
   - Selector format file (`JPEG`, `PNG`, `WEBP`) dan slider kompresi kualitas (`50%` s/d `100%`) dengan tombol CTA "Simpan Sebagai Berkas Baru".

---

## 🔒 UI System Hidden Vault & App Lock (`HiddenVaultScreen`)

1. **BiometricPrompt Dialog**: Modul autentikasi standar Android OS untuk verifikasi sidik jari/wajah.
2. **Keypad PIN Fallback**:
   - Indikator 4-digit titik PIN di bagian atas.
   - Keypad angka 3x4 yang besar dan mudah ditekan dengan umpan balik getaran (*haptic feedback*).
3. **Vault Grid Isolation**:
   - Tampilan grid foto dalam vault dilengkapi badge ikon gembok kecil di setiap thumbnail.
   - Tombol utama "Restore ke Galeri" dan "Hapus Permanen" di BottomBar.

---

## ⚓ UI System Floating Dock Navigation (`FloatingDock.kt`)

1. **Capsule Glassmorphism Layout**:
   - Komponen melayang berbentuk pill/kapsul di bagian bawah tengah layar (`NavigationBar` melayang).
   - Menggunakan warna Surface Variant dengan transparansi 85% dan border halus `surfaceTint`.
2. **Media Type Filter Control**:
   - Tombol navigasi pertama bertransformasi secara melingkar (`ALL` ➔ `PHOTOS` ➔ `VIDEOS` ➔ `ALL`) dengan ikon semantik (`PermMedia`, `Image`, `Videocam`) dan label dinamis (`Semua`, `Foto`, `Video`).
3. **Auto-Hide & Horizontal Scroll Action Mode**:
   - Otomatis menyembunyikan posisi pill saat pengguna melakukan gulir (*scroll*) ke bawah pada grid.
   - Transisi animasi dari mode navigasi utama menjadi Action Bar interaktif dengan `Modifier.horizontalScroll` saat mode multi-select aktif.

---

## 🔍 UI System Duplicate Photo Finder (`DuplicateScreen.kt`)

1. **Grouping Card Container**:
   - Setiap kelompok foto mirip disajikan dalam `Card` terpisah dengan header jumlah item dan estimasi ruang penyimpanan yang dapat dihemat.
2. **Best Quality Badge**:
   - Foto dengan ukuran berkas terbesar secara otomatis mendapatkan penanda (*badge*) visual *"Kualitas Terbaik (Pertahankan)"*.
3. **One-tap Bulk Cleanup Floating Bar**:
   - Tombol utama *"✨ Bersihkan Semua"* yang menonjol di bagian bawah untuk menghapus seluruh duplikat secara massal tanpa memilih manual.

---

## 🎬 UI System Internal Video Player (`VideoPlayerScreen.kt`)

1. **Embedded Player Overlay**:
   - Kontrol pemutaran video (`Play/Pause`, `SeekBar`, `Time Duration`) muncul/sembunyi saat layar di-tap.
2. **Auto Lifecycle Binding**:
   - Pemutaran video berhenti dan instance ExoPlayer dilepas secara otomatis saat pengguna menekan tombol kembali.

---

## ⚙️ UI System Settings & Theme Customization (`SettingsScreen`)

1. **Material 3 Accent Color Selector**:
   - Grid 4 pilihan warna aksen (Emerald Green, Ocean Blue, Sunset Orange, Royal Purple) dengan indikator centang pada warna aktif.
2. **Preferences Section Cards**:
   - Kartu preferensi terkelompok: Tampilan & Tema, Filter & Media (Default Startup, Auto-play Video, Mute Audio), Keamanan & Vault (Lock Delay & Ubah PIN), serta Manajemen Storage & Cache.
3. **Storage & Cache Cleaner Action**:
   - Indikator ukuran disk usage (Room DB, Coil cache, Vault storage) dan Button `primaryContainer` *"Bersihkan Cache Thumbnail"* 1-klik.

---

## 📊 UI System Sort By & 1-Click Bulk Select (`SortBottomSheet.kt`)

1. **Sort BottomSheet Dialog**:
   - ModalBottomSheet Material 3 yang menyajikan opsi pengurutan (Terbaru, Terlama, Nama A-Z, Nama Z-A, Ukuran Terbesar, Ukuran Terkecil, Bulan & Tahun) dengan indikator `RadioButton`.
2. **1-Click Selection & Action Bar**:
   - Ikon TopBar `Checklist` memicu mode seleksi tanpa gesture long-press.
   - Ikon `DoneAll` pada `FloatingDockActionBar` beralih instan antara *Select All* (pilih seluruh loaded items) dan *Deselect All*.

---

## ❌ Anti-Patterns (Yang Harus Dihindari)

| ❌ Dilarang | ✅ Seharusnya |
|---|---|
| Menggunakan XML Layouts | 100% Jetpack Compose deklaratif |
| Memuat dekode bitmap ukuran asli di Grid | Batasi ke 256px thumbnail via Coil |
| Menimpa file asli pengguna saat edit | Simpan hasil edit sebagai file baru di MediaStore |
| Menulis thumbnail Vault ke penyimpanan publik | Dekode sementara di RAM selama sesi Vault |
| Hardcoded warna RGB di komponen Compose | Gunakan `MaterialTheme.colorScheme.*` |

---

*Single Source of Truth untuk: Gallery App Android — Jetpack Compose + Material 3*
*Terakhir diperbarui: 29 Juli 2026 | v1.14.2 (versionCode 45)*
