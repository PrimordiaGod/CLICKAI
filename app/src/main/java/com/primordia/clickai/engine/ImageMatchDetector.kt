package com.primordia.clickai.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import kotlin.math.sqrt

class ImageMatchDetector(
    private val screenshotProvider: () -> Bitmap?,
) {
    private var templateProvider: ((String) -> Bitmap?)? = null

    fun setTemplateProvider(provider: (String) -> Bitmap?) { templateProvider = provider }

    fun findTemplateCenter(templateAsset: String, threshold: Float, region: Region?, multiScale: Boolean = true): PointF? {
        val loader = templateProvider ?: return null
        val tpl = loader(templateAsset) ?: return null
        return findTemplateCenter(tpl, threshold, region, multiScale)
    }

    fun findTemplateCenter(template: Bitmap, threshold: Float, region: Region?, multiScale: Boolean = true): PointF? {
        val screenshot = screenshotProvider() ?: return null
        val search = if (region != null) Bitmap.createBitmap(
            screenshot,
            region.left.coerceAtLeast(0),
            region.top.coerceAtLeast(0),
            (region.right - region.left).coerceAtMost(screenshot.width - region.left),
            (region.bottom - region.top).coerceAtMost(screenshot.height - region.top)
        ) else screenshot
        return try {
            val scales = if (multiScale) floatArrayOf(1.0f, 0.9f, 0.8f, 0.7f, 1.1f) else floatArrayOf(1.0f)
            var bestScore = -1.0
            var bestPoint: PointF? = null
            for (scale in scales) {
                val tpl = if (scale != 1.0f) Bitmap.createScaledBitmap(template, (template.width * scale).toInt(), (template.height * scale).toInt(), true) else template
                val match = matchNcc(search, tpl)
                if (match != null && match.score > bestScore) {
                    bestScore = match.score
                    bestPoint = PointF((match.x + tpl.width / 2f) + (region?.left ?: 0), (match.y + tpl.height / 2f) + (region?.top ?: 0))
                }
                if (tpl !== template && !tpl.isRecycled) tpl.recycle()
            }
            if (bestScore >= threshold) bestPoint else null
        } catch (t: Throwable) {
            Log.w("ClickAI", "Image match failed: ${t.message}")
            null
        } finally {
            if (search !== screenshot && !search.isRecycled) search.recycle()
        }
    }

    private data class MatchResult(val x: Int, val y: Int, val score: Double)

    private fun matchNcc(search: Bitmap, template: Bitmap): MatchResult? {
        if (template.width > search.width || template.height > search.height) return null
        val tplPixels = IntArray(template.width * template.height)
        val srcPixels = IntArray(search.width * search.height)
        template.getPixels(tplPixels, 0, template.width, 0, 0, template.width, template.height)
        search.getPixels(srcPixels, 0, search.width, 0, 0, search.width, search.height)
        fun lum(c: Int): Int { val r=(c shr 16) and 0xFF; val g=(c shr 8) and 0xFF; val b=c and 0xFF; return (0.299*r + 0.587*g + 0.114*b).toInt() }
        val tplY = IntArray(tplPixels.size) { lum(tplPixels[it]) }
        val tplMean = tplY.average()
        val tplStd = sqrt(tplY.fold(0.0) { acc, v -> acc + (v - tplMean) * (v - tplMean) } / tplY.size)
        if (tplStd == 0.0) return null
        var best = MatchResult(0, 0, -1.0)
        val n = template.width * template.height
        for (y in 0..(search.height - template.height)) {
            for (x in 0..(search.width - template.width)) {
                var sum = 0.0
                var sumSq = 0.0
                var cross = 0.0
                var idxTpl = 0
                for (j in 0 until template.height) {
                    val rowOff = (y + j) * search.width + x
                    for (i in 0 until template.width) {
                        val s = lum(srcPixels[rowOff + i])
                        val t = tplY[idxTpl++]
                        sum += s
                        sumSq += s * s
                        cross += (s) * (t - tplMean)
                    }
                }
                val mean = sum / n
                val std = sqrt(sumSq / n - mean * mean)
                if (std == 0.0) continue
                val score = cross / (n * std * tplStd)
                if (score > best.score) best = MatchResult(x, y, score)
            }
        }
        return best
    }
}