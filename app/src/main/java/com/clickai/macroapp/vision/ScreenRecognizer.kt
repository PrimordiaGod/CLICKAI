package com.clickai.macroapp.vision

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

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
        // OpenCV not available; stub always returns false
        return false
    }
}