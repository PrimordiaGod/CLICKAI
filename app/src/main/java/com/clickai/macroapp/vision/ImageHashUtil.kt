package com.clickai.macroapp.vision

import android.graphics.Bitmap
import android.graphics.Color
import java.security.MessageDigest

object ImageHashUtil {
    // Simple average hash (aHash) for demo; can be replaced with pHash
    fun averageHash(bitmap: Bitmap): String {
        val size = 8
        val resized = Bitmap.createScaledBitmap(bitmap, size, size, false)
        var sum = 0L
        val pixels = IntArray(size * size)
        resized.getPixels(pixels, 0, size, 0, 0, size, size)
        for (p in pixels) sum += Color.red(p) + Color.green(p) + Color.blue(p)
        val avg = sum / (size * size * 3)
        val hash = StringBuilder()
        for (p in pixels) {
            val v = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
            hash.append(if (v > avg) '1' else '0')
        }
        return hash.toString()
    }

    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}