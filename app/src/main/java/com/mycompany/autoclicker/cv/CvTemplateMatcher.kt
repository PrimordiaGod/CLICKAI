package com.mycompany.autoclicker.cv

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object CvTemplateMatcher {

    init {
        if (!OpenCVLoader.initLocal()) {
            throw RuntimeException("Failed to load OpenCV native libs")
        }
        System.loadLibrary("autoclicker_native")
    }

    private external fun matchTemplateNative(srcMatAddr: Long, templMatAddr: Long): FloatArray

    data class Result(val score: Float, val x: Int, val y: Int)

    /**
     * Perform template matching returning best match score and location (top-left).
     */
    fun match(source: Bitmap, template: Bitmap): Result {
        val src = Mat()
        Utils.bitmapToMat(source, src)
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
        val tmpl = Mat()
        Utils.bitmapToMat(template, tmpl)
        Imgproc.cvtColor(tmpl, tmpl, Imgproc.COLOR_RGBA2GRAY)
        val res = matchTemplateNative(src.nativeObjAddr, tmpl.nativeObjAddr)
        return Result(res[0], res[1].toInt(), res[2].toInt())
    }

    data class MultiScaleResult(val score: Float, val x: Int, val y: Int, val scale: Float)

    /**
     * Multi-scale template matching. Scales the template from [minScale] to [maxScale] (inclusive)
     * with the provided [step]. Returns the best match across scales.
     */
    fun matchMultiScale(
        source: Bitmap,
        template: Bitmap,
        minScale: Float = 0.8f,
        maxScale: Float = 1.2f,
        step: Float = 0.05f
    ): MultiScaleResult {
        val src = Mat()
        Utils.bitmapToMat(source, src)
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)

        var bestScore = -1f
        var bestX = 0
        var bestY = 0
        var bestScale = 1f

        var scale = minScale
        while (scale <= maxScale + 1e-3) {
            val tmplScaled = Mat()
            val newWidth = (template.width * scale).toInt().coerceAtLeast(1)
            val newHeight = (template.height * scale).toInt().coerceAtLeast(1)
            val tmpBitmap = Bitmap.createScaledBitmap(template, newWidth, newHeight, true)
            Utils.bitmapToMat(tmpBitmap, tmplScaled)
            Imgproc.cvtColor(tmplScaled, tmplScaled, Imgproc.COLOR_RGBA2GRAY)

            val res = matchTemplateNative(src.nativeObjAddr, tmplScaled.nativeObjAddr)
            val score = res[0]
            if (score > bestScore) {
                bestScore = score
                bestX = res[1].toInt()
                bestY = res[2].toInt()
                bestScale = scale
            }

            scale += step
        }

        return MultiScaleResult(bestScore, bestX, bestY, bestScale)
    }
}