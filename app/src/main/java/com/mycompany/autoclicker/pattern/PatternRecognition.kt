package com.mycompany.autoclicker.pattern

import android.graphics.Bitmap
import android.graphics.Rect
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.macro.Condition
import com.mycompany.autoclicker.ocr.OcrEngine
import kotlinx.coroutines.runBlocking

sealed class Element {
    data class Template(val bitmap: Bitmap, val threshold: Float = 0.8f) : Element()
    data class Text(val regex: Regex) : Element()
    data class Color(val region: Rect, val argb: Int, val tol: Int = 10) : Element()
}

enum class SpatialRelation { BELOW, ABOVE, RIGHT_OF, LEFT_OF }

data class PatternRule(val anchor: Element, val relative: Element, val relation: SpatialRelation)

data class Pattern(val rules: List<PatternRule>)

data class PatternMatch(val anchorRect: Rect, val relativeRect: Rect, val score: Float)

object PatternRecognizer {
    /**
     * Very simple implementation: only supports Template anchor and Template relative with BELOW relation.
     */
    suspend fun detect(frame: Bitmap, pattern: Pattern): PatternMatch? {
        if (pattern.rules.isEmpty()) return null
        val rule = pattern.rules.first()
        if (rule.anchor !is Element.Template || rule.relative !is Element.Template) return null
        val anchorRes = CvTemplateMatcher.matchMultiScale(frame, rule.anchor.bitmap)
        if (anchorRes.score < rule.anchor.threshold) return null
        val anchorRect = Rect(anchorRes.x, anchorRes.y,
            (anchorRes.x + rule.anchor.bitmap.width * anchorRes.scale).toInt(),
            (anchorRes.y + rule.anchor.bitmap.height * anchorRes.scale).toInt())

        // search region below anchor
        val searchRegionTop = anchorRect.bottom
        val searchRegion = Rect(0, searchRegionTop, frame.width, (searchRegionTop + frame.height * 0.4).toInt().coerceAtMost(frame.height))
        val subBitmap = Bitmap.createBitmap(frame, searchRegion.left, searchRegion.top, searchRegion.width(), searchRegion.height())
        val relRes = CvTemplateMatcher.matchMultiScale(subBitmap, rule.relative.bitmap)
        if (relRes.score < rule.relative.threshold) return null
        val relRect = Rect(
            searchRegion.left + relRes.x,
            searchRegion.top + relRes.y,
            searchRegion.left + relRes.x + (rule.relative.bitmap.width * relRes.scale).toInt(),
            searchRegion.top + relRes.y + (rule.relative.bitmap.height * relRes.scale).toInt()
        )
        return PatternMatch(anchorRect, relRect, (anchorRes.score + relRes.score) / 2f)
    }
}