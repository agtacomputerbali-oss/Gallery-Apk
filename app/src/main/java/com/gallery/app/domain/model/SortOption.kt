package com.gallery.app.domain.model

enum class SortOption(val displayName: String) {
    DATE_TAKEN_DESC("Terbaru (Tanggal)"),
    DATE_TAKEN_ASC("Terlama (Tanggal)"),
    MONTH_DESC("Bulan & Tahun"),
    YEAR_DESC("Tahun"),
    DISPLAY_NAME_ASC("Nama (A-Z)"),
    DISPLAY_NAME_DESC("Nama (Z-A)"),
    SIZE_DESC("Ukuran (Terbesar)"),
    SIZE_ASC("Ukuran (Terkecil)")
}
