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
}