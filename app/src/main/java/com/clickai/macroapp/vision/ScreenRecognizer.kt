package com.clickai.macroapp.vision

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.android.Utils

object ScreenRecognizer {
    private var tessBaseAPI: TessBaseAPI? = null

    fun initTesseract(context: Context, lang: String = "eng") {
        if (tessBaseAPI == null) {
            tessBaseAPI = TessBaseAPI()
            val dir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
            tessBaseAPI?.init(dir, lang)
        }
    }

    fun recognizeText(bitmap: Bitmap): String {
        tessBaseAPI?.setImage(bitmap)
        return tessBaseAPI?.utF8Text ?: ""
    }

    fun matchTemplate(screen: Bitmap, template: Bitmap, threshold: Double = 0.9): Boolean {
        val screenMat = Mat()
        val templateMat = Mat()
        Utils.bitmapToMat(screen, screenMat)
        Utils.bitmapToMat(template, templateMat)
        val resultCols = screenMat.cols() - templateMat.cols() + 1
        val resultRows = screenMat.rows() - templateMat.rows() + 1
        val result = Mat(resultRows, resultCols, org.opencv.core.CvType.CV_32FC1)
        Imgproc.matchTemplate(screenMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)
        val mmr = Core.minMaxLoc(result)
        return mmr.maxVal >= threshold
    }
}