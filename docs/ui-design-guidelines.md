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

## 🎨 UI System Custom Image Editor (`EditorScreen`)

1. **Canvas Crop Overlay**:
   - Menampilkan kerangka garis potong (*crop handles*) di atas foto.
   - Mendukung penyesuaian rasio instan (Bebas, 1:1, 4:3) via tombol tab horizontal.
2. **Carousel Filter Preview**:
   - Baris kompresi horizontal di bagian bawah editor yang menampilkan *mini-thumbnail* foto dengan efek `ColorMatrix` real-time (Original, Grayscale, Sepia, Warm, Cool, Contrast).
3. **Action Footer**: Tombol "Batal" (kiri) dan "Simpan Sebagai Baru" (kanan - Primary CTA).

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
*Terakhir diperbarui: 26 Juli 2026*
