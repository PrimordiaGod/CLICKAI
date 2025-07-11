package com.mycompany.autoclicker.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OcrEngine private constructor(private val tess: TessBaseAPI) {

    suspend fun recognize(bitmap: Bitmap, roi: Rect? = null): String = withContext(Dispatchers.Default) {
        val cropped = if (roi != null) Bitmap.createBitmap(bitmap, roi.left, roi.top, roi.width(), roi.height()) else bitmap
        tess.setImage(cropped)
        tess.utF8Text ?: ""
    }

    companion object {
        @Volatile private var instance: OcrEngine? = null
        private const val LANG = "eng"

        suspend fun get(context: Context): OcrEngine {
            return instance ?: synchronized(this) {
                instance ?: withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "tessdata")
                    if (!dir.exists()) dir.mkdirs()
                    val trainedData = File(dir, "$LANG.traineddata")
                    if (!trainedData.exists()) {
                        context.assets.open("tessdata/$LANG.traineddata").use { input ->
                            FileOutputStream(trainedData).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    val api = TessBaseAPI()
                    api.init(context.filesDir.absolutePath, LANG)
                    OcrEngine(api).also { instance = it }
                }
            }
        }
    }
}