package com.gallery.app.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.cos

object PHashCalculator {

    private const val SIZE = 32
    private const val SMALL_SIZE = 8

    /**
     * Menghitung 64-bit Perceptual Hash (pHash) berbasis DCT dari Bitmap.
     * Mengembalikan 16 karakter Hexadecimal String (misal: "a3f01b9e82c74d0f").
     */
    fun calculatePHash(sourceBitmap: Bitmap): String {
        // 1. Resize ke 32x32
        val scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, SIZE, SIZE, true)

        // 2. Konversi ke matriks grayscale 32x32
        val vals = Array(SIZE) { DoubleArray(SIZE) }
        for (x in 0 until SIZE) {
            for (y in 0 until SIZE) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                vals[x][y] = 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        if (scaledBitmap != sourceBitmap) {
            scaledBitmap.recycle()
        }

        // 3. Hitung 2D DCT
        val dct = applyDCT(vals)

        // 4. Hitung Rata-rata dari 8x8 matriks teratas (abaikan DC coeff [0][0])
        var total = 0.0
        for (x in 0 until SMALL_SIZE) {
            for (y in 0 until SMALL_SIZE) {
                if (x == 0 && y == 0) continue
                total += dct[x][y]
            }
        }
        val avg = total / (SMALL_SIZE * SMALL_SIZE - 1)

        // 5. Buat 64-bit string (1 jika > avg, 0 jika <= avg)
        val binary = StringBuilder()
        for (x in 0 until SMALL_SIZE) {
            for (y in 0 until SMALL_SIZE) {
                binary.append(if (dct[x][y] > avg) "1" else "0")
            }
        }

        // 6. Konversi 64-bit binary ke 16-char Hex String
        return binaryToHex(binary.toString())
    }

    /**
     * Menghitung Hamming Distance antara dua pHash Hex String.
     * Mengembalikan jumlah bit yang berbeda (0 s/d 64).
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) return 64

        var dist = 0
        val b1 = hexToBinary(hash1)
        val b2 = hexToBinary(hash2)

        val length = minOf(b1.length, b2.length)
        for (i in 0 until length) {
            if (b1[i] != b2[i]) {
                dist++
            }
        }
        return dist
    }

    private fun applyDCT(f: Array<DoubleArray>): Array<DoubleArray> {
        val F = Array(SIZE) { DoubleArray(SIZE) }
        val c = DoubleArray(SIZE) { if (it == 0) 1.0 / Math.sqrt(2.0) else 1.0 }

        for (u in 0 until SIZE) {
            for (v in 0 until SIZE) {
                var sum = 0.0
                for (i in 0 until SIZE) {
                    for (j in 0 until SIZE) {
                        sum += f[i][j] *
                                cos((2 * i + 1) * u * Math.PI / (2.0 * SIZE)) *
                                cos((2 * j + 1) * v * Math.PI / (2.0 * SIZE))
                    }
                }
                F[u][v] = 0.25 * c[u] * c[v] * sum
            }
        }
        return F
    }

    private fun binaryToHex(binary: String): String {
        val hex = StringBuilder()
        for (i in binary.indices step 4) {
            val chunk = binary.substring(i, minOf(i + 4, binary.length))
            val decimal = chunk.toInt(2)
            hex.append(decimal.toString(16))
        }
        return hex.toString()
    }

    private fun hexToBinary(hex: String): String {
        val binary = StringBuilder()
        for (ch in hex) {
            val decimal = ch.toString().toInt(16)
            val binChunk = Integer.toBinaryString(decimal).padStart(4, '0')
            binary.append(binChunk)
        }
        return binary.toString()
    }
}
