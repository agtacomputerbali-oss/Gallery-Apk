# Panduan Build & Signing Release APK / AAB — Gallery App Android

Dokumen ini berisi instruksi dan best-practices untuk melakukan kompilasi, penandatanganan berkas (*signing*), dan pengoptimalan aplikasi **Gallery App Android** ke dalam bentuk **APK Release** atau **Android App Bundle (AAB)** untuk produksi.

> **Versi Dokumen:** v1.0.0-MVP — 26 Juli 2026

---

## 1. Konfigurasi Signing Key & Keystore

Untuk mengompilasi APK Release yang siap di-install di Perangkat atau diunggah ke Google Play Store, Anda memerlukan berkas *Keystore* penandatangan digital.

### A. Persiapan Keystore (`keystore.jks`)
Buat keystore rilis jika belum ada (gunakan Java `keytool`):
```bash
keytool -genkey -v -keystore release-key.jks -alias gallery-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

### B. Simpan Kredensial di `local.properties` (Aman / Non-Git)
Tambahkan informasi sensitif ke berkas `local.properties` (jangan di-commit ke Git):
```properties
RELEASE_STORE_FILE=../release-key.jks
RELEASE_STORE_PASSWORD=PasswordKeystoreAnda
RELEASE_KEY_ALIAS=gallery-key-alias
RELEASE_KEY_PASSWORD=PasswordAliasAnda
```

### C. Konfigurasi `app/build.gradle.kts`
Pastikan konfigurasi `signingConfigs` membaca kredensial dari `local.properties`:

```kotlin
import java.util.Properties

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE") ?: "release-key.jks")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## 2. ProGuard / R8 Rules (`proguard-rules.pro`)

Pengoptimalan R8 diaktifkan (`isMinifyEnabled = true`) untuk memotong kode mati (*dead code elimination*) dan mengamankan berkas binary. Pastikan aturan ProGuard mencakup library berikut:

```proguard
# Jetpack Compose Rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Hilt / Dependency Injection
-keep class com.gallery.app.di.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Coil 3 Image Loading
-keep class coil3.** { *; }

# Encrypted DataStore & Security Crypto
-keep class androidx.security.crypto.** { *; }
```

---

## 3. Eksekusi Build Production

Jalankan perintah Gradle berikut dari terminal proyek:

### A. Build APK Release (Siap Install di Perangkat)
```bash
./gradlew assembleRelease
```
*Hasil output*: `app/build/outputs/apk/release/app-release.apk`

### B. Build Android App Bundle (AAB - Untuk Play Store)
```bash
./gradlew bundleRelease
```
*Hasil output*: `app/build/outputs/bundle/release/app-release.aab`

---

## 4. Verifikasi & Pengujian Akhir

Sebelum mempublikasikan berkas APK/AAB:
1. **Uji Cold Start di Perangkat Fisik**: Install APK di perangkat fisik Android 13+ dan Android 8-12 untuk memverifikasi bahwa aplikasi dapat terbuka tanpa crash saat *cold start*.
2. **Verifikasi Izin Runtime**: Pastikan dialog izin `READ_MEDIA_IMAGES` atau `READ_EXTERNAL_STORAGE` dapat muncul dengan benar.
3. **Verifikasi Features**: Uji scroll grid galeri, perbesaran foto (*pinch-zoom*), editor gambar (crop + filter), pemindahan ke sampah (*trash*), dan otentikasi biometrik pada bilik rahasia (*vault*).

---

*Terakhir diperbarui: 26 Juli 2026*
