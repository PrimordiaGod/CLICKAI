package com.mycompany.autoclicker.pattern

import android.graphics.Bitmap
import android.graphics.Rect
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.ocr.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ConcurrentHashMap

data class PatternMatch(
    val confidence: Float,
    val region: Rect,
    val patternType: PatternType,
    val metadata: Map<String, Any> = emptyMap()
)

enum class PatternType {
    TEMPLATE_MATCH,
    COLOR_PATTERN,
    TEXT_PATTERN,
    GESTURE_PATTERN,
    LAYOUT_PATTERN,
    ANIMATION_PATTERN
}

data class PatternDefinition(
    val id: String,
    val name: String,
    val type: PatternType,
    val template: Bitmap? = null,
    val colorRanges: List<ColorRange> = emptyList(),
    val textPatterns: List<Regex> = emptyList(),
    val layoutConstraints: LayoutConstraints? = null,
    val gesturePattern: GesturePattern? = null,
    val threshold: Float = 0.8f,
    val priority: Int = 0
)

data class ColorRange(
    val hsvMin: ColorDetector.HSV,
    val hsvMax: ColorDetector.HSV,
    val region: Rect? = null,
    val minPixels: Int = 1
)

data class LayoutConstraints(
    val relativePositions: List<RelativePosition> = emptyList(),
    val sizeConstraints: SizeConstraints? = null,
    val alignment: Alignment? = null
)

data class RelativePosition(
    val elementId: String,
    val direction: Direction,
    val distance: Float,
    val tolerance: Float = 10f
)

enum class Direction {
    ABOVE, BELOW, LEFT, RIGHT, NEAR
}

data class SizeConstraints(
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null,
    val aspectRatio: Float? = null
)

data class Alignment(
    val horizontal: HorizontalAlignment? = null,
    val vertical: VerticalAlignment? = null
)

enum class HorizontalAlignment { LEFT, CENTER, RIGHT }
enum class VerticalAlignment { TOP, CENTER, BOTTOM }

data class GesturePattern(
    val points: List<Point>,
    val timeSequence: List<Long>,
    val tolerance: Float = 20f
)

class PatternRecognitionEngine {
    private val patterns = ConcurrentHashMap<String, PatternDefinition>()
    private val matchCache = ConcurrentHashMap<String, List<PatternMatch>>()
    private val frameHistory = mutableListOf<Bitmap>()
    private val maxHistorySize = 10

    fun registerPattern(pattern: PatternDefinition) {
        patterns[pattern.id] = pattern
        matchCache.clear() // Clear cache when patterns change
    }

    fun unregisterPattern(id: String) {
        patterns.remove(id)
        matchCache.remove(id)
    }

    suspend fun detectPatterns(frame: Bitmap): List<PatternMatch> = withContext(Dispatchers.Default) {
        updateFrameHistory(frame)
        
        val allMatches = mutableListOf<PatternMatch>()
        
        patterns.values.sortedByDescending { it.priority }.forEach { pattern ->
            val matches = when (pattern.type) {
                PatternType.TEMPLATE_MATCH -> detectTemplatePattern(frame, pattern)
                PatternType.COLOR_PATTERN -> detectColorPattern(frame, pattern)
                PatternType.TEXT_PATTERN -> detectTextPattern(frame, pattern)
                PatternType.GESTURE_PATTERN -> detectGesturePattern(frame, pattern)
                PatternType.LAYOUT_PATTERN -> detectLayoutPattern(frame, pattern)
                PatternType.ANIMATION_PATTERN -> detectAnimationPattern(frame, pattern)
            }
            allMatches.addAll(matches)
        }
        
        allMatches.sortedByDescending { it.confidence }
    }

    private fun detectTemplatePattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        val template = pattern.template ?: return emptyList()
        
        return try {
            val result = CvTemplateMatcher.matchMultiScale(frame, template, 0.5f, 1.5f, 0.1f)
            if (result.score >= pattern.threshold) {
                listOf(PatternMatch(
                    confidence = result.score,
                    region = Rect(result.x, result.y, 
                        (result.x + template.width * result.scale).toInt(),
                        (result.y + template.height * result.scale).toInt()),
                    patternType = PatternType.TEMPLATE_MATCH,
                    metadata = mapOf("scale" to result.scale)
                ))
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun detectColorPattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        
        pattern.colorRanges.forEach { colorRange ->
            val region = colorRange.region ?: Rect(0, 0, frame.width, frame.height)
            
            if (ColorDetector.isHsvColorPresent(frame, region, colorRange.hsvMin, colorRange.hsvMax)) {
                // Count matching pixels for confidence calculation
                val matchingPixels = countMatchingPixels(frame, region, colorRange.hsvMin, colorRange.hsvMax)
                val totalPixels = region.width() * region.height()
                val confidence = (matchingPixels.toFloat() / totalPixels).coerceIn(0f, 1f)
                
                if (matchingPixels >= colorRange.minPixels) {
                    matches.add(PatternMatch(
                        confidence = confidence,
                        region = region,
                        patternType = PatternType.COLOR_PATTERN,
                        metadata = mapOf(
                            "matchingPixels" to matchingPixels,
                            "totalPixels" to totalPixels
                        )
                    ))
                }
            }
        }
        
        return matches
    }

    private suspend fun detectTextPattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        val matches = mutableListOf<PatternMatch>()
        
        try {
            val ocrEngine = OcrEngine.get(frame.context)
            val text = ocrEngine.recognize(frame)
            
            pattern.textPatterns.forEach { regex ->
                val matchResult = regex.find(text)
                if (matchResult != null) {
                    // For now, return the entire frame as region
                    // In a more sophisticated implementation, you'd get bounding boxes for text
                    matches.add(PatternMatch(
                        confidence = 0.9f, // High confidence for exact regex match
                        region = Rect(0, 0, frame.width, frame.height),
                        patternType = PatternType.TEXT_PATTERN,
                        metadata = mapOf(
                            "matchedText" to matchResult.value,
                            "fullText" to text
                        )
                    ))
                }
            }
        } catch (e: Exception) {
            // OCR failed, return empty list
        }
        
        return matches
    }

    private fun detectGesturePattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        // This would require tracking touch events over time
        // For now, return empty list
        return emptyList()
    }

    private fun detectLayoutPattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        val constraints = pattern.layoutConstraints ?: return emptyList()
        
        // This is a simplified implementation
        // A full implementation would analyze the layout structure
        return listOf(PatternMatch(
            confidence = 0.7f,
            region = Rect(0, 0, frame.width, frame.height),
            patternType = PatternType.LAYOUT_PATTERN
        ))
    }

    private fun detectAnimationPattern(frame: Bitmap, pattern: PatternDefinition): List<PatternMatch> {
        if (frameHistory.size < 2) return emptyList()
        
        val currentFrame = frameHistory.last()
        val previousFrame = frameHistory[frameHistory.size - 2]
        
        // Calculate frame difference to detect motion
        val motionScore = calculateMotionScore(previousFrame, currentFrame)
        
        if (motionScore > 0.1f) { // Threshold for motion detection
            return listOf(PatternMatch(
                confidence = motionScore.coerceIn(0f, 1f),
                region = Rect(0, 0, frame.width, frame.height),
                patternType = PatternType.ANIMATION_PATTERN,
                metadata = mapOf("motionScore" to motionScore)
            ))
        }
        
        return emptyList()
    }

    private fun countMatchingPixels(bitmap: Bitmap, region: Rect, hsvMin: ColorDetector.HSV, hsvMax: ColorDetector.HSV): Int {
        val mat = Mat()
        val roi = Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
        Utils.bitmapToMat(roi, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2HSV)
        
        val mask = Mat()
        Core.inRange(mat, Scalar(hsvMin.h, hsvMin.s, hsvMin.v), Scalar(hsvMax.h, hsvMax.s, hsvMax.v), mask)
        
        return Core.countNonZero(mask)
    }

    private fun calculateMotionScore(frame1: Bitmap, frame2: Bitmap): Float {
        val mat1 = Mat()
        val mat2 = Mat()
        
        Utils.bitmapToMat(frame1, mat1)
        Utils.bitmapToMat(frame2, mat2)
        
        Imgproc.cvtColor(mat1, mat1, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_RGBA2GRAY)
        
        val diff = Mat()
        Core.absdiff(mat1, mat2, diff)
        
        val mean = Core.mean(diff)
        return (mean.`val`[0] / 255.0).toFloat()
    }

    private fun updateFrameHistory(frame: Bitmap) {
        frameHistory.add(frame)
        if (frameHistory.size > maxHistorySize) {
            frameHistory.removeAt(0)
        }
    }

    fun clearCache() {
        matchCache.clear()
        frameHistory.clear()
    }

    fun getRegisteredPatterns(): List<PatternDefinition> {
        return patterns.values.toList()
    }
}