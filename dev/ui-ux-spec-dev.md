# UI/UX Specification — Media Filter, Settings Hub & Photo Editor

## 1. Floating Dock & Universal Media Filter Layout

### Floating Dock Navigation Bar Component
- **Tombol Pertama**: Transformasi dari tombol static `Foto` menjadi **Media Type Filter Control**.
- **Indikator Visual**:
  - `ALL` (Semua Media): Icon `Icons.Default.PermMedia` / `Collections`, Label `"Semua"`.
  - `PHOTOS` (Foto Saja): Icon `Icons.Default.Image`, Label `"Foto"`.
  - `VIDEOS` (Video Saja): Icon `Icons.Default.Videocam` / `VideoLibrary`, Label `"Video"`.
- **Interaksi Pengguna**:
  - Single Click: Berganti mode filter secara melingkar (`Semua` ➔ `Foto` ➔ `Video` ➔ `Semua`).
  - Visual Feedback: Getaran haptic `HapticFeedbackType.LongPress` saat pergantian filter.

---

## 2. Fitur Settings Hub (Layar Pengaturan Aplikasi)

### Section Layout
1. **Tampilan & Tema**:
   - Radio group Tema (Ikuti Sistem / Terang / Gelap).
   - Selector Warna Aksen Material 3 (Emerald Green / Ocean Blue / Sunset Orange / Royal Purple).
   - Grid Column Selector (3, 4, 5, 6 kolom) per layar.
2. **Preferensi Filter & Media**:
   - Segmented Button `Default Filter` (`Semua`, `Foto`, `Video`).
   - Switch `Auto-play Video Preview` dan Switch `Mute Audio Default`.
3. **Keamanan & Vault**:
   - Dropdown / Radio `Vault Lock Delay` (`Seketika`, `30 Detik`, `1 Menit`, `5 Menit`).
   - Tombol Action `Ubah PIN Vault`.
4. **Manajemen Storage & Cache**:
   - Kartu Informasi Disk Usage (Metadata Cache Room DB, Coil Image Cache Size in MB).
   - Action Button `Bersihkan Cache Thumbnail` (dengan indikator status).

---

## 3. Editor Foto Professional Toolbar Layout (`EditorScreen.kt`)

### Bottom Navigation Tabs Editor
- **Tab Bar Komponen**:
  1. 📐 **Crop & Ratio**: Options Free, 1:1, 4:3, 16:9.
  2. 🎛️ **Penyetelan (Adjustments)**: Sliders Horizontal (Brightness, Contrast, Saturation, Temperature, Vignette).
  3. 🔄 **Rotasi & Flip**: Buttons Rotate L (90° CCW), Rotate R (90° CW), Flip Horizontal, Flip Vertical.
  4. 🎨 **Preset Filter**: Horizontal Scrollable Thumbnails (Original, Vivid, Warm Vintage, Cool Breeze, Dramatic B&W, Noir, Sepia, Cinema, Pastel, Cyberpunk).
  5. ✏️ **Doodle & Teks**: Palette Warna Brush, Stroke Width Slider, Undo Brush, Add Text Input.

### Action Bar & Export Dialog
- Top Bar: Tombol Batal (`Cancel`), Tombol Undo/Redo, Tombol Simpan (`Save`).
- Bottom Sheet Export: Selector Format (`JPEG`, `PNG`, `WEBP`) + Slider Kualitas Kompresi (50% s/d 100%).
