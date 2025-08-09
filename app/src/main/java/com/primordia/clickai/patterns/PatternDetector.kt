package com.primordia.clickai.patterns

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.gson.Gson
import com.primordia.clickai.engine.ImageMatchDetector
import com.primordia.clickai.engine.OcrDetector

class PatternDetector(
    private val screenshotProvider: () -> Bitmap?,
    private val ocr: OcrDetector,
    private val img: ImageMatchDetector,
    private val loadPatternByName: (String) -> Pattern?
) {
    private val gson = Gson()

    fun detectByName(name: String): Boolean {
        val p = loadPatternByName(name) ?: return false
        return detect(p)
    }

    fun detect(pattern: Pattern): Boolean {
        val bmp = screenshotProvider() ?: return false
        // Element detections -> bounding boxes
        val boxes = mutableMapOf<String, android.graphics.Rect>()
        for (e in pattern.elements) {
            when (e) {
                is ImageElement -> {
                    // Needs a way to load template bitmap from assetPath; skip if not available
                    // Assume not detected in scaffolding: return false
                    return false
                }
                is TextElement -> {
                    val ok = ocr.containsText(e.query, null, e.isRegex)
                    if (!ok) return false
                    // Without exact boxes from OCR, we pass presence check only
                }
                is ColorElement -> {
                    // Color presence handled at macro level; here we assume pass-through
                }
            }
        }
        // Spatial relations would need element boxes; in scaffolding we only validate presence
        return true
    }
}