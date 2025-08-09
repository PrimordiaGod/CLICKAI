package com.primordia.clickai.engine

import android.graphics.Bitmap
import android.graphics.Color

class ColorDetector(
    private val screenshotProvider: () -> Bitmap?
) {
    fun containsColor(region: Region, color: Int, tolerance: Int): Boolean {
        val bmp = screenshotProvider() ?: return false
        val l = region.left.coerceAtLeast(0)
        val t = region.top.coerceAtLeast(0)
        val r = region.right.coerceAtMost(bmp.width)
        val b = region.bottom.coerceAtMost(bmp.height)
        if (l >= r || t >= b) return false
        val w = r - l
        val h = b - t
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, l, t, w, h)
        val tr = Color.red(color); val tg = Color.green(color); val tb = Color.blue(color)
        val tol = tolerance.coerceAtLeast(0)
        for (p in pixels) {
            val dr = kotlin.math.abs(Color.red(p) - tr)
            val dg = kotlin.math.abs(Color.green(p) - tg)
            val db = kotlin.math.abs(Color.blue(p) - tb)
            if (dr <= tol && dg <= tol && db <= tol) return true
        }
        return false
    }
}