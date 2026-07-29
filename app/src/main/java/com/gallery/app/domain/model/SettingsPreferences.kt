package com.gallery.app.domain.model

enum class VaultLockDelay(val displayName: String, val seconds: Long) {
    IMMEDIATELY("Seketika saat keluar", 0L),
    SECONDS_30("30 Detik", 30L),
    MINUTES_1("1 Menit", 60L),
    MINUTES_5("5 Menit", 300L)
}

enum class AccentColor(val displayName: String, val primaryColorHex: Long) {
    DEFAULT_EMERALD("Emerald Green", 0xFF2E7D32),
    OCEAN_BLUE("Ocean Blue", 0xFF1565C0),
    SUNSET_ORANGE("Sunset Orange", 0xFFE65100),
    ROYAL_PURPLE("Royal Purple", 0xFF6A1B9A)
}
