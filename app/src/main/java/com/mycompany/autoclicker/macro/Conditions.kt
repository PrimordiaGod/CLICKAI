package com.mycompany.autoclicker.macro

import android.graphics.Bitmap
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.ocr.OcrEngine
import kotlinx.coroutines.runBlocking

sealed class Condition {
    abstract suspend fun eval(frame: Bitmap): Boolean

    class TemplatePresent(private val template: Bitmap, private val threshold: Float = 0.8f) : Condition() {
        override suspend fun eval(frame: Bitmap): Boolean {
            val res = CvTemplateMatcher.matchMultiScale(frame, template)
            return res.score >= threshold
        }
    }

    class TextMatches(private val regex: Regex, private val ocrEngineProvider: suspend () -> OcrEngine) : Condition() {
        override suspend fun eval(frame: Bitmap): Boolean {
            val ocr = ocrEngineProvider()
            val text = ocr.recognize(frame)
            return regex.containsMatchIn(text)
        }
    }

    class ColorPresent(private val region: android.graphics.Rect, private val color: Int, private val tol: Int = 10) : Condition() {
        override suspend fun eval(frame: Bitmap): Boolean {
            return ColorDetector.isRgbColorPresent(frame, region, color, tol)
        }
    }
}