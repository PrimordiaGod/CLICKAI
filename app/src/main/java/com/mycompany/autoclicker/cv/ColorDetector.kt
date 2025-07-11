package com.mycompany.autoclicker.cv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object ColorDetector {

    init {
        // Ensure OpenCV libs are loaded – shared with TemplateMatcher
        org.opencv.android.OpenCVLoader.initLocal()
    }

    data class HSV(val h: Double, val s: Double, val v: Double)

    /** Convert ARGB color int to HSV triple */
    fun rgbToHsv(color: Int): HSV {
        val hsvArr = FloatArray(3)
        Color.colorToHSV(color, hsvArr)
        return HSV(hsvArr[0].toDouble(), hsvArr[1].toDouble() * 255, hsvArr[2].toDouble() * 255)
    }

    /** Sample pixel color at (x,y) from bitmap (ARGB int) */
    fun sampleColor(bitmap: Bitmap, x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= bitmap.width || y >= bitmap.height) return 0
        return bitmap.getPixel(x, y)
    }

    /** Check if target RGB color appears within tolerance (per-channel diff) inside region. */
    fun isRgbColorPresent(bitmap: Bitmap, region: Rect, targetColor: Int, tol: Int = 10): Boolean {
        val regionClamped = Rect(
            0.coerceAtLeast(region.left),
            0.coerceAtLeast(region.top),
            bitmap.width.coerceAtMost(region.right),
            bitmap.height.coerceAtMost(region.bottom)
        )
        val pixels = IntArray(regionClamped.width() * regionClamped.height())
        bitmap.getPixels(pixels, 0, regionClamped.width(), regionClamped.left, regionClamped.top, regionClamped.width(), regionClamped.height())
        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)
        for (c in pixels) {
            if (Math.abs(Color.red(c) - tr) <= tol &&
                Math.abs(Color.green(c) - tg) <= tol &&
                Math.abs(Color.blue(c) - tb) <= tol
            ) return true
        }
        return false
    }

    /** HSV in range 0-180 for H, 0-255 for S,V (OpenCV convention). */
    fun isHsvColorPresent(bitmap: Bitmap, region: Rect, hsvMin: HSV, hsvMax: HSV): Boolean {
        val mat = Mat()
        val roi = Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
        Utils.bitmapToMat(roi, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2HSV)
        val mask = Mat()
        Core.inRange(mat, Scalar(hsvMin.h, hsvMin.s, hsvMin.v), Scalar(hsvMax.h, hsvMax.s, hsvMax.v), mask)
        val nonZero = Core.countNonZero(mask)
        return nonZero > 0
    }
}