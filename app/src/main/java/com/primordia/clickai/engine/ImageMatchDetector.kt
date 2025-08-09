package com.primordia.clickai.engine

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import com.primordia.clickai.util.BitmapUtils

class ImageMatchDetector(
    private val screenshotProvider: () -> Bitmap?
) {
    // Placeholder: integrate OpenCV's matchTemplate (TM_CCOEFF_NORMED).
    // For now, returns null to avoid false positives; replace with JNI/AAR bindings.

    fun findTemplateCenter(templateAsset: String, threshold: Float, region: Region?): PointF? {
        val screenshot = screenshotProvider() ?: return null
        val cropped = if (region != null) {
            val rect = Rect(region.left, region.top, region.right, region.bottom)
            BitmapUtils.safeCrop(screenshot, rect)
        } else screenshot

        return try {
            Log.d("ClickAI", "matchTemplate stub: $templateAsset threshold=$threshold region=$region")
            null
        } catch (t: Throwable) {
            Log.w("ClickAI", "Image match failed: ${t.message}")
            null
        } finally {
            if (cropped !== screenshot) cropped.recycle()
        }
    }
}