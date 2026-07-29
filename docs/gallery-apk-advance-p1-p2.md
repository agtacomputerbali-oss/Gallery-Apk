# 🗺️ Roadmap Pengembangan OP Gallery — Fase P1 & P2
Dokumen ini berisi rincian rencana pengembangan untuk Fase P1 (Foundation & Caching) dan Fase P2 (Multimedia & Organization) sebagai langkah lanjutan dari MVP Scope P0.

**Versi Dokumen:** v1.3.0 | **Status:** Planned / In Progress

---

##  Fase P1: Foundation & Caching (Fondasi & Performa)
**Tujuan Utama:** Meningkatkan performa aplikasi secara drastis saat menangani puluhan ribu foto, menghilangkan *delay* saat *initial load*, dan mempersiapkan fondasi untuk pemrosesan latar belakang (AI/ML).

### 🛠️ Fitur & Modul yang Dikembangkan
1. **Room Database Caching (Metadata Indexing)**
   - **Konsep:** Membuat *local cache* menggunakan **Room Database** untuk metadata `MediaStore`.
   - **Manfaat:** Membuat *initial load* galeri menjadi instan (0ms delay) dan memungkinkan pencarian/filtering offline yang super cepat tanpa membebani `ContentResolver` berulang kali.
   - **Implementasi:** Sinkronisasi data antara `MediaStore` dan `Room` menggunakan `ContentObserver` untuk memantau perubahan secara *real-time*.

2. **WorkManager for Background Indexing**
   - **Konsep:** Menggunakan **WorkManager** untuk mengeksekusi tugas berat di latar belakang.
   - **Tugas:** Indexing metadata, *pre-generate* thumbnail video, dan persiapan fondasi untuk *duplicate finder*.
   - **Manfaat:** Tidak memblokir *Main Thread*, menjaga UI tetap 60/120fps, dan menghemat baterai dengan menjadwalkan tugas saat perangkat *idle* atau *charging*.

### 🧰 Tech Stack Tambahan (Fase P1)
- **Room Database** (`androidx.room:room-runtime`, `room-ktx`, `room-compiler`)
- **WorkManager** (`androidx.work:work-runtime-ktx`)
- **ContentObserver** (Untuk memantau perubahan `MediaStore` secara reaktif)

---

## 🎬 Fase P2: Multimedia & Organization (Multimedia & Organisasi Cerdas)
**Tujuan Utama:** Mengubah aplikasi dari sekadar penampil foto menjadi *all-in-one media hub*, memberikan alat manajemen penyimpanan yang cerdas, serta meningkatkan pengalaman pengguna melalui desain UI/UX modern yang imersif.

### 🛠️ Fitur & Modul yang Dikembangkan

#### 1. 🎨 Floating Dock Navigation (Pengganti TopAppBar)
- **Konsep:** Mengganti `TopAppBar` tradisional dengan **Floating Dock** berbentuk *pill/capsule* yang melayang di bagian bawah layar, mengikuti tren desain modern (iOS Dynamic Island, macOS Dock).
- **Fitur:**
  - Navigasi utama: **Albums**, **Trash**, **Vault**, **Settings** — diakses via ikon dengan label opsional.
  - **Immersive Grid:** Grid foto menjadi *full-bleed* dari atas ke bawah, membuat foto menjadi *hero* utama aplikasi.
  - **Thumb-Friendly:** Posisi di bawah lebih ergonomis untuk perangkat layar besar.
  - **Smart Transformation:** Dock berubah otomatis menjadi **Action Bar** saat mode *multi-select* aktif (Share, Delete, Move to Vault, Cancel).
  - **Auto-Hide on Scroll:** Dock dapat disembunyikan otomatis saat pengguna *scroll* ke bawah, dan muncul kembali saat *scroll* ke atas.
- **Manfaat:** Tampilan lebih modern, premium, dan fokus pada konten foto. Meningkatkan UX secara signifikan.
- **Implementasi Teknis:**
  - Komponen `FloatingDock.kt` dengan `Card` berbentuk `RoundedCornerShape(32.dp)` dan *glassmorphism* effect (`surface.copy(alpha = 0.85f)`).
  - Integrasi dengan `Box(modifier = Modifier.fillMaxSize())` sebagai overlay di atas `PhotoGrid`.
  - Animasi transisi menggunakan `animateContentSize()` dan `AnimatedVisibility`.
  - Dukungan *safe area* untuk menghindari konflik dengan gesture bar Android.
  - *Haptic feedback* saat ikon dock ditekan.
  - *Content description* lengkap untuk aksesibilitas (TalkBack).

#### 2. Media3 / ExoPlayer Integration (Advanced Video)
- **Konsep:** Mengintegrasikan **Jetpack Media3** untuk pemutaran video internal yang mulus.
- **Fitur:** Mendukung format HDR, *gesture* kontrol (swipe untuk seek, double tap untuk skip), dan *Picture-in-Picture* (PiP).
- **Manfaat:** Pengalaman menonton video yang jauh lebih baik, stabil, dan terintegrasi dengan UI aplikasi dibanding melempar ke pemutar video bawaan sistem.

#### 3. Duplicate & Similar Photo Finder
- **Konsep:** Mendeteksi foto duplikat atau sangat mirip menggunakan *Perceptual Hashing (pHash)*.
- **Fitur:** Membandingkan *hash* gambar, menampilkan grup foto mirip, dan menyediakan tombol *one-tap cleanup* untuk menghapus file yang redundan.
- **Manfaat:** Langsung memberikan nilai guna tinggi dengan membantu pengguna menghemat ruang penyimpanan (Storage) secara signifikan.

#### 4. Smart Albums (Album Otomatis)
- **Konsep:** Membuat album dinamis berdasarkan metadata EXIF/GPS atau tipe file.
- **Kategori:** "Selfie" (berdasarkan kamera depan), "Screenshots", "Panorama", "Video", dll.
- **Manfaat:** Merapikan galeri secara otomatis tanpa pengguna perlu membuat album manual.

### 🧰 Tech Stack Tambahan (Fase P2)
- **Jetpack Media3** (`androidx.media3:media3-exoplayer`, `media3-ui`, `media3-common`)
- **ImageHash / pHash Library** (Untuk kompresi perseptual dan pencarian gambar mirip)
- **ExifInterface** (`androidx.exifinterface:exifinterface`) untuk pembacaan metadata kamera secara mendalam.
- **Material 3 Animation** (`animateContentSize`, `AnimatedVisibility`) untuk transisi Floating Dock.
- **HapticFeedback** (`LocalHapticFeedback`) untuk umpan balik taktil pada dock.

---

## 📝 Catatan Implementasi & Strategi
- **Ketergantungan Fase:** Fase P1 adalah prasyarat mutlak untuk Fase P2 dan P3. Tanpa *caching* Room, fitur *Duplicate Finder* dan *Smart Albums* akan sangat lambat dan berisiko menyebabkan *Out Of Memory* (OOM) pada perangkat dengan >10.000 foto.
- **Prinsip Keamanan:** Seluruh pengembangan tetap berpegang pada prinsip **Scoped Storage Compliance**, **Pencegahan OOM**, dan **Imutabilitas File Asli**.
- **Monetisasi (Opsional):** Fitur *Duplicate Finder* dan *Media3 Player* dapat dijadikan sebagai fitur *Premium/Pro* jika aplikasi ditujukan untuk model bisnis berbayar.
- **UX Consistency:** Floating Dock harus konsisten di seluruh layar utama (Gallery, Albums, Trash, Vault) untuk menjaga pengalaman pengguna yang seragam.