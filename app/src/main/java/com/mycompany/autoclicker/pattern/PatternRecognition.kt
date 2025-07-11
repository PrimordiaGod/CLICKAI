package com.mycompany.autoclicker.pattern

import android.graphics.Bitmap
import android.graphics.Rect
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.ocr.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Element {
    data class Template(val bitmap: Bitmap, val threshold: Float = 0.8f) : Element()
    data class Text(val regex: Regex) : Element()
    data class Color(val region: Rect, val argb: Int, val tol: Int = 10) : Element()
}

enum class SpatialRelation { BELOW, ABOVE, RIGHT_OF, LEFT_OF }

data class PatternRule(val anchor: Element, val relative: Element, val relation: SpatialRelation)

data class Pattern(val rules: List<PatternRule>) {
    fun allElements() = rules.flatMap { listOf(it.anchor, it.relative) }.distinct()
}

data class PatternMatch(val matchedRects: Map<Element, Rect>, val avgScore: Float)

object PatternRecognizer {

    private data class Hit(val rect: Rect, val score: Float)

    suspend fun detect(frame: Bitmap, pattern: Pattern): PatternMatch? {
        // step1 detect hits for each element
        val elementHits: MutableMap<Element, List<Hit>> = mutableMapOf()
        for (elem in pattern.allElements()) {
            val hits = withContext(Dispatchers.Default) { detectElement(frame, elem) }
            if (hits.isEmpty()) return null
            elementHits[elem] = hits
        }
        // DFS combinations
        val matchedRects: MutableMap<Element, Rect> = mutableMapOf()
        val matchedScores: MutableMap<Element, Float> = mutableMapOf()
        val elements = pattern.allElements()
        return dfs(0, elements, elementHits, pattern.rules, matchedRects, matchedScores)
    }

    private fun dfs(idx: Int,
                     order: List<Element>,
                     hits: Map<Element, List<Hit>>,
                     rules: List<PatternRule>,
                     chosenRects: MutableMap<Element, Rect>,
                     chosenScores: MutableMap<Element, Float>): PatternMatch? {
        if (idx == order.size) {
            // validate all rules satisfied
            for (rule in rules) {
                val rA = chosenRects[rule.anchor] ?: return null
                val rB = chosenRects[rule.relative] ?: return null
                if (!satisfies(rA, rB, rule.relation)) return null
            }
            val avg = chosenScores.values.average().toFloat()
            return PatternMatch(HashMap(chosenRects), avg)
        }
        val elem = order[idx]
        for (hit in hits.getValue(elem)) {
            chosenRects[elem] = hit.rect
            chosenScores[elem] = hit.score
            val res = dfs(idx + 1, order, hits, rules, chosenRects, chosenScores)
            if (res != null) return res
            chosenRects.remove(elem)
            chosenScores.remove(elem)
        }
        return null
    }

    private fun satisfies(a: Rect, b: Rect, rel: SpatialRelation): Boolean = when (rel) {
        SpatialRelation.BELOW    -> b.top >= a.bottom
        SpatialRelation.ABOVE    -> b.bottom <= a.top
        SpatialRelation.RIGHT_OF -> b.left >= a.right
        SpatialRelation.LEFT_OF  -> b.right <= a.left
    }

    private fun detectElement(frame: Bitmap, elem: Element): List<Hit> = when (elem) {
        is Element.Template -> {
            val m = CvTemplateMatcher.matchMultiScale(frame, elem.bitmap)
            if (m.score >= elem.threshold) listOf(Hit(Rect(m.x, m.y, (m.x + elem.bitmap.width * m.scale).toInt(), (m.y + elem.bitmap.height * m.scale).toInt()), m.score)) else emptyList()
        }
        is Element.Text -> {
            val text = runCatching { OcrEngine.get(AppContextHolder.app).recognize(frame) }.getOrDefault("")
            if (elem.regex.containsMatchIn(text)) listOf(Hit(Rect(0,0,frame.width,frame.height),1f)) else emptyList()
        }
        is Element.Color -> {
            if (ColorDetector.isRgbColorPresent(frame, elem.region, elem.argb, elem.tol)) listOf(Hit(elem.region,1f)) else emptyList()
        }
    }
}