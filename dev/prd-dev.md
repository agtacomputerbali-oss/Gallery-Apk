# Master PRD — Android Gallery APK

## Overview
Aplikasi OP Gallery adalah aplikasi galeri media modern berbasis Android (Jetpack Compose + Material 3 + Clean Architecture + Hilt).

## Requirements & Spesifikasi Modul Baru

### 1. Media Type Filtering di Floating Dock & Universal Screen Filtering (P0 - High)
- **User Story**: Sebagai pengguna, saya ingin memfilter tampilan media (Semua / Foto Saja / Video Saja) melalui Floating Dock secara konsisten di seluruh layar galeri.
- **Scope & Interaksi**:
  - Tombol pertama di Floating Dock bertransformasi dari sekadar tombol "Foto" static menjadi **Media Type Filter Control** (`ALL` / `PHOTOS_ONLY` / `VIDEOS_ONLY`).
  - Menampilkan ikon & label dinamis (`Semua`, `Foto`, `Video`). Single-tap berpindah filter secara melingkar.
  - **Layar Utama (GalleryHomeScreen)**: Memfilter `MediaPagingSource` via `MediaStore` query. Untuk memuat video & foto, query WAJIB membaca `MediaStore.Files.getContentUri("external")` atau mengombinasikan `MediaStore.Images.Media` dan `MediaStore.Video.Media` (bukan hanya `MediaStore.Images.Media`).
  - **Layar Detail Album (AlbumDetailScreen)**: Memfilter media dalam album/folder tertentu.
  - **Layar Hidden Vault (VaultScreen)**: Memfilter media tersembunyi di Vault berdasarkan `mimeType`.

---

### 2. Fitur Settings / Pengaturan Aplikasi (P1 - High)
- **User Story**: Sebagai pengguna, saya ingin menyesuaikan preferensi aplikasi, keamanan vault, tampilan tema, serta mengelola cache memori.
- **Fitur & Spesifikasi**:
  1. **Default Media Filter pada Startup**: Preferensi filter awal saat aplikasi dibuka (`ALL`, `PHOTOS_ONLY`, `VIDEOS_ONLY`) tersimpan di DataStore.
  2. **Preferensi Pemutaran Video**:
     - `Auto-play Video` (On/Off): Memutar video otomatis saat peninjau media dibuka.
     - `Mute Video by Default` (On/Off): Membungkam suara video saat pertama diputar.
  3. **Keamanan Vault & Lock Delay**:
     - *Vault Lock Delay*: Pilihan tenggang waktu penguncian otomatis Vault (`IMMEDIATELY`, `SECONDS_30`, `MINUTES_1`, `MINUTES_5`).
     - *Pintasan Ganti PIN*: Dialog perubahan PIN Vault terenkripsi.
  4. **Kustomisasi Tema & Warna Aksen**:
     - *Mode Tema*: Ikuti Sistem, Terang, Gelap.
     - *Warna Aksen Material 3*: Emerald Green (Bawaan), Ocean Blue, Sunset Orange, Royal Purple.
  5. **Storage & Cache Cleaner Hub**:
     - Ringkasan statistik ukuran cache Room DB index, Coil disk cache, dan Vault storage.
     - Tombol 1-klik `Bersihkan Cache Thumbnail` untuk mengosongkan cache Coil tanpa menghapus data asli.

---

### 3. Fitur Photo Editing / Editor Foto (P1 - High)
- **User Story**: Sebagai pengguna, saya ingin mengedit foto secara profesional dengan pratinjau real-time (live preview), alat penyetelan, rotasi, flip, overlay crop, preset filter visual, coretan lukis, teks, serta memilih format simpan.
- **Fitur & Spesifikasi**:
  1. **Real-time Live Preview & Overlay Crop**:
     - Canvas pratinjau WAJIB merespons state editan secara real-time (`ColorMatrixColorFilter`, `graphicsLayer` rotation/flip, `CropOverlay`, doodle/text path).
     - Integration `CropOverlay`: Memasangkan grid overlay crop interaktif saat tab Crop aktif.
  1. **Advanced Adjustments (Penyetelan Detail)**:
     - **Brightness**: Slider -100 s/d +100.
     - **Contrast**: Slider -100 s/d +100.
     - **Saturation**: Slider -100 s/d +100.
     - **Warmth / Temperature**: Slider -100 s/d +100.
     - **Vignette**: Slider 0 s/d 100.
  2. **Transformasi Geometri (Rotate & Flip)**:
     - Rotasi 90° Searah & Berlawanan Jarum Jam.
     - Flip Horizontal (Cermin Kiri-Kanan) & Flip Vertical (Cermin Atas-Bawah).
  3. **Pustaka Preset Filter Visual (10+ Presets)**:
     - *Original*, *Vivid*, *Warm Vintage*, *Cool Breeze*, *Dramatic B&W*, *Noir*, *Sepia*, *Cinematic*, *Pastel*, *Cyberpunk*.
  4. **Doodle & Markup (Gambar Lukis & Teks)**:
     - *Freehand Brush Tool*: Lukisan bebas dengan pilihan warna (Color Picker) & ukuran garis (Stroke Width slider).
     - *Text Overlay*: Penambahan teks kustom di atas foto.
  5. **Pilihan Format & Kualitas Ekspor**:
     - Format simpan: `JPEG`, `PNG`, `WEBP`.
     - Slider kualitas kompresi: `50%` s/d `100%`.
  6. **Lokasi Penyimpanan Foto Hasil Edit**:
     - Foto editan WAJIB disimpan di folder/album asal yang sama dengan foto yang diedit (membaca `RELATIVE_PATH` media asal, misal `Pictures/Screenshots`, `DCIM/Camera`, `Download`). Fallback ke `DCIM/Camera` jika path asal tidak terdeteksi.
