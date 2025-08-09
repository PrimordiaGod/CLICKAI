package com.primordia.clickai.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.primordia.clickai.util.BitmapUtils

class OcrDetector(
    private val screenshotProvider: () -> Bitmap?
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun containsText(query: String, region: Region?): Boolean {
        val bmp = screenshotProvider() ?: return false
        val cropped = if (region != null) {
            val rect = Rect(region.left, region.top, region.right, region.bottom)
            BitmapUtils.safeCrop(bmp, rect)
        } else bmp

        return try {
            val image = InputImage.fromBitmap(cropped, 0)
            val result = Tasks.await(recognizer.process(image))
            flattenText(result).contains(query, ignoreCase = true)
        } catch (t: Throwable) {
            Log.w("ClickAI", "OCR failed: ${t.message}")
            false
        } finally {
            if (cropped !== bmp) cropped.recycle()
        }
    }

    private fun flattenText(text: Text): String = buildString {
        for (block in text.textBlocks) {
            for (line in block.lines) {
                appendLine(line.text)
            }
        }
    }
}