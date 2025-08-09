package com.primordia.clickai.util

import android.graphics.*

object BitmapUtils {
    fun safeCrop(src: Bitmap, rect: Rect): Bitmap {
        val safe = Rect(
            rect.left.coerceAtLeast(0),
            rect.top.coerceAtLeast(0),
            rect.right.coerceAtMost(src.width),
            rect.bottom.coerceAtMost(src.height)
        )
        if (safe.width() <= 0 || safe.height() <= 0) return src
        return Bitmap.createBitmap(src, safe.left, safe.top, safe.width(), safe.height())
    }

    fun downscale(src: Bitmap, maxW: Int = 1080, maxH: Int = 1920): Bitmap {
        if (src.width <= maxW && src.height <= maxH) return src
        val ratio = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    fun toGrayscale(src: Bitmap): Bitmap {
        val bmpGrayscale = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmpGrayscale
    }
}