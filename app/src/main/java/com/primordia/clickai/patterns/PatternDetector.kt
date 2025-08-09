package com.primordia.clickai.patterns

import android.graphics.Bitmap
import android.graphics.Rect
import com.primordia.clickai.engine.ColorDetector
import com.primordia.clickai.engine.ImageMatchDetector
import com.primordia.clickai.engine.OcrDetector

class PatternDetector(
    private val screenshotProvider: () -> Bitmap?,
    private val ocr: OcrDetector,
    private val img: ImageMatchDetector,
    private val loadPatternByName: (String) -> Pattern?,
    private val color: ColorDetector? = null
) {
    fun detectByName(name: String): Boolean {
        val p = loadPatternByName(name) ?: return false
        return detect(p)
    }

    fun detect(pattern: Pattern): Boolean {
        val bmp = screenshotProvider() ?: return false
        val boxes = mutableMapOf<String, Rect>()
        for (e in pattern.elements) {
            when (e) {
                is ImageElement -> {
                    // TODO: Load template bitmap and use img.findTemplateCenter(templateBitmap,...)
                    return false
                }
                is TextElement -> {
                    val lines = ocr.extractLines(null) ?: return false
                    val match = if (e.isRegex) {
                        val rx = Regex(e.query, RegexOption.IGNORE_CASE)
                        lines.firstOrNull { rx.containsMatchIn(it.first) }
                    } else {
                        lines.firstOrNull { it.first.contains(e.query, ignoreCase = true) }
                    }
                    if (match == null) return false
                    boxes[e.id] = match.second
                }
                is ColorElement -> {
                    val ok = color?.containsColor(e.region.toEngineRegion(), e.color, e.tolerance) ?: false
                    if (!ok) return false
                    boxes[e.id] = e.region.toAndroidRect()
                }
            }
        }
        // Check relations
        for (rel in pattern.relations) {
            val a = boxes[rel.fromId] ?: return false
            val b = boxes[rel.toId] ?: return false
            if (!satisfies(a, b, rel)) return false
        }
        return true
    }

    private fun satisfies(a: Rect, b: Rect, rel: Relation): Boolean {
        return when (rel.type) {
            RelationType.ABOVE -> a.bottom + rel.minPx <= b.top && a.bottom + rel.maxPx >= b.top
            RelationType.BELOW -> a.top >= b.bottom + rel.minPx && a.top <= b.bottom + rel.maxPx
            RelationType.LEFT_OF -> a.right + rel.minPx <= b.left && a.right + rel.maxPx >= b.left
            RelationType.RIGHT_OF -> a.left >= b.right + rel.minPx && a.left <= b.right + rel.maxPx
        }
    }
}

private fun Region.toAndroidRect(): Rect = Rect(left, top, right, bottom)
private fun Region.toEngineRegion(): com.primordia.clickai.engine.Region = com.primordia.clickai.engine.Region(left, top, right, bottom)