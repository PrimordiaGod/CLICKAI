package com.mycompany.autoclicker.macro

import android.graphics.Bitmap
import android.graphics.Rect
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.ocr.OcrEngine
import com.mycompany.autoclicker.pattern.PatternMatch
import com.mycompany.autoclicker.pattern.PatternRecognitionEngine
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

sealed class AdvancedCondition {
    abstract suspend fun eval(frame: Bitmap): Boolean
    abstract fun getDescription(): String
}

// Composite conditions
data class AndCondition(val conditions: List<AdvancedCondition>) : AdvancedCondition() {
    override suspend fun eval(frame: Bitmap): Boolean {
        return conditions.all { it.eval(frame) }
    }
    
    override fun getDescription(): String {
        return "AND(${conditions.joinToString(", ") { it.getDescription() }})"
    }
}

data class OrCondition(val conditions: List<AdvancedCondition>) : AdvancedCondition() {
    override suspend fun eval(frame: Bitmap): Boolean {
        return conditions.any { it.eval(frame) }
    }
    
    override fun getDescription(): String {
        return "OR(${conditions.joinToString(", ") { it.getDescription() }})"
    }
}

data class NotCondition(val condition: AdvancedCondition) : AdvancedCondition() {
    override suspend fun eval(frame: Bitmap): Boolean {
        return !condition.eval(frame)
    }
    
    override fun getDescription(): String {
        return "NOT(${condition.getDescription()})"
    }
}

// Time-based conditions
data class TimeCondition(
    val startTime: Long? = null,
    val endTime: Long? = null,
    val intervalMs: Long? = null,
    val lastExecution: AtomicLong = AtomicLong(0)
) : AdvancedCondition() {
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val now = System.currentTimeMillis()
        
        // Check time window
        if (startTime != null && now < startTime) return false
        if (endTime != null && now > endTime) return false
        
        // Check interval
        if (intervalMs != null) {
            val last = lastExecution.get()
            if (now - last < intervalMs) return false
            lastExecution.set(now)
        }
        
        return true
    }
    
    override fun getDescription(): String {
        return "TimeCondition(start=$startTime, end=$endTime, interval=$intervalMs)"
    }
}

// Count-based conditions
data class CountCondition(
    val targetCount: Int,
    val currentCount: AtomicInteger = AtomicInteger(0),
    val resetOnSuccess: Boolean = true
) : AdvancedCondition() {
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val current = currentCount.get()
        if (current >= targetCount) {
            if (resetOnSuccess) {
                currentCount.set(0)
            }
            return true
        }
        currentCount.incrementAndGet()
        return false
    }
    
    override fun getDescription(): String {
        return "CountCondition(current=${currentCount.get()}/$targetCount)"
    }
}

// Pattern-based conditions
data class PatternCondition(
    val patternEngine: PatternRecognitionEngine,
    val patternId: String,
    val minConfidence: Float = 0.8f
) : AdvancedCondition() {
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val matches = patternEngine.detectPatterns(frame)
        return matches.any { it.confidence >= minConfidence }
    }
    
    override fun getDescription(): String {
        return "PatternCondition(pattern=$patternId, minConfidence=$minConfidence)"
    }
}

// Advanced template conditions
data class MultiTemplateCondition(
    val templates: List<Bitmap>,
    val minMatches: Int = 1,
    val threshold: Float = 0.8f
) : AdvancedCondition() {
    
    override suspend fun eval(frame: Bitmap): Boolean {
        var matchCount = 0
        for (template in templates) {
            try {
                val result = CvTemplateMatcher.matchMultiScale(frame, template)
                if (result.score >= threshold) {
                    matchCount++
                }
            } catch (e: Exception) {
                // Continue with next template
            }
        }
        return matchCount >= minMatches
    }
    
    override fun getDescription(): String {
        return "MultiTemplateCondition(templates=${templates.size}, minMatches=$minMatches)"
    }
}

// Color pattern conditions
data class ColorPatternCondition(
    val colorRanges: List<ColorRange>,
    val regions: List<Rect> = emptyList(),
    val requireAll: Boolean = false
) : AdvancedCondition() {
    
    data class ColorRange(
        val hsvMin: ColorDetector.HSV,
        val hsvMax: ColorDetector.HSV,
        val minPixels: Int = 1
    )
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val regionsToCheck = if (regions.isEmpty()) {
            listOf(Rect(0, 0, frame.width, frame.height))
        } else {
            regions
        }
        
        var matchCount = 0
        for (region in regionsToCheck) {
            for (colorRange in colorRanges) {
                if (ColorDetector.isHsvColorPresent(frame, region, colorRange.hsvMin, colorRange.hsvMax)) {
                    matchCount++
                    if (!requireAll) break // Found at least one match
                }
            }
        }
        
        return if (requireAll) {
            matchCount >= colorRanges.size
        } else {
            matchCount > 0
        }
    }
    
    override fun getDescription(): String {
        return "ColorPatternCondition(ranges=${colorRanges.size}, requireAll=$requireAll)"
    }
}

// Text-based conditions with advanced matching
data class AdvancedTextCondition(
    val patterns: List<TextPattern>,
    val requireAll: Boolean = false,
    val caseSensitive: Boolean = false
) : AdvancedCondition() {
    
    data class TextPattern(
        val regex: Regex,
        val minOccurrences: Int = 1,
        val maxOccurrences: Int = Int.MAX_VALUE
    )
    
    override suspend fun eval(frame: Bitmap): Boolean {
        try {
            val ocrEngine = OcrEngine.get(frame.context)
            val text = ocrEngine.recognize(frame)
            val searchText = if (caseSensitive) text else text.lowercase()
            
            var matchCount = 0
            for (pattern in patterns) {
                val matches = pattern.regex.findAll(searchText).toList()
                if (matches.size in pattern.minOccurrences..pattern.maxOccurrences) {
                    matchCount++
                    if (!requireAll) break // Found at least one match
                }
            }
            
            return if (requireAll) {
                matchCount >= patterns.size
            } else {
                matchCount > 0
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    override fun getDescription(): String {
        return "AdvancedTextCondition(patterns=${patterns.size}, requireAll=$requireAll)"
    }
}

// Gesture-based conditions
data class GestureCondition(
    val expectedGesture: List<Point>,
    val tolerance: Float = 20f,
    val timeWindow: Long = 5000L // 5 seconds
) : AdvancedCondition() {
    
    data class Point(val x: Float, val y: Float)
    
    private val gestureHistory = mutableListOf<Point>()
    private val lastGestureTime = AtomicLong(0)
    
    fun addGesturePoint(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        if (now - lastGestureTime.get() > timeWindow) {
            gestureHistory.clear()
        }
        gestureHistory.add(Point(x, y))
        lastGestureTime.set(now)
    }
    
    override suspend fun eval(frame: Bitmap): Boolean {
        // This would need to be integrated with touch event tracking
        // For now, return false as gesture detection requires touch event handling
        return false
    }
    
    override fun getDescription(): String {
        return "GestureCondition(points=${expectedGesture.size}, tolerance=$tolerance)"
    }
}

// Performance-based conditions
data class PerformanceCondition(
    val maxDetectionTime: Long = 1000L, // 1 second
    val minFps: Float = 10f,
    val maxMemoryUsage: Long = 100 * 1024 * 1024L // 100MB
) : AdvancedCondition() {
    
    private val lastDetectionTime = AtomicLong(0)
    private val fpsHistory = mutableListOf<Float>()
    private val maxFpsHistorySize = 10
    
    fun updateMetrics(detectionTime: Long, fps: Float, memoryUsage: Long) {
        lastDetectionTime.set(detectionTime)
        fpsHistory.add(fps)
        if (fpsHistory.size > maxFpsHistorySize) {
            fpsHistory.removeAt(0)
        }
    }
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val detectionTime = lastDetectionTime.get()
        val avgFps = fpsHistory.average().toFloat()
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        return detectionTime <= maxDetectionTime &&
               avgFps >= minFps &&
               memoryUsage <= maxMemoryUsage
    }
    
    override fun getDescription(): String {
        return "PerformanceCondition(maxTime=${maxDetectionTime}ms, minFps=$minFps, maxMemory=${maxMemoryUsage}bytes)"
    }
}

// State-based conditions
data class StateCondition(
    val stateName: String,
    val expectedValue: Any,
    val stateManager: StateManager
) : AdvancedCondition() {
    
    override suspend fun eval(frame: Bitmap): Boolean {
        val currentValue = stateManager.getState(stateName)
        return currentValue == expectedValue
    }
    
    override fun getDescription(): String {
        return "StateCondition($stateName = $expectedValue)"
    }
}

// State management for complex conditions
class StateManager {
    private val states = mutableMapOf<String, Any>()
    
    fun setState(name: String, value: Any) {
        states[name] = value
    }
    
    fun getState(name: String): Any? {
        return states[name]
    }
    
    fun clearState(name: String) {
        states.remove(name)
    }
    
    fun clearAllStates() {
        states.clear()
    }
}

// Condition builder for easy creation
class ConditionBuilder {
    private val conditions = mutableListOf<AdvancedCondition>()
    
    fun template(template: Bitmap, threshold: Float = 0.8f): ConditionBuilder {
        conditions.add(object : AdvancedCondition() {
            override suspend fun eval(frame: Bitmap): Boolean {
                val result = CvTemplateMatcher.matchMultiScale(frame, template)
                return result.score >= threshold
            }
            override fun getDescription(): String = "TemplateCondition(threshold=$threshold)"
        })
        return this
    }
    
    fun text(regex: Regex, caseSensitive: Boolean = false): ConditionBuilder {
        conditions.add(AdvancedTextCondition(
            patterns = listOf(AdvancedTextCondition.TextPattern(regex)),
            caseSensitive = caseSensitive
        ))
        return this
    }
    
    fun color(region: Rect, color: Int, tolerance: Int = 10): ConditionBuilder {
        conditions.add(object : AdvancedCondition() {
            override suspend fun eval(frame: Bitmap): Boolean {
                return ColorDetector.isRgbColorPresent(frame, region, color, tolerance)
            }
            override fun getDescription(): String = "ColorCondition(color=#$color, tolerance=$tolerance)"
        })
        return this
    }
    
    fun time(startTime: Long? = null, endTime: Long? = null, intervalMs: Long? = null): ConditionBuilder {
        conditions.add(TimeCondition(startTime, endTime, intervalMs))
        return this
    }
    
    fun count(targetCount: Int, resetOnSuccess: Boolean = true): ConditionBuilder {
        conditions.add(CountCondition(targetCount, resetOnSuccess = resetOnSuccess))
        return this
    }
    
    fun and(): ConditionBuilder {
        if (conditions.size >= 2) {
            val lastTwo = conditions.takeLast(2)
            conditions.removeAt(conditions.size - 1)
            conditions.removeAt(conditions.size - 1)
            conditions.add(AndCondition(lastTwo))
        }
        return this
    }
    
    fun or(): ConditionBuilder {
        if (conditions.size >= 2) {
            val lastTwo = conditions.takeLast(2)
            conditions.removeAt(conditions.size - 1)
            conditions.removeAt(conditions.size - 1)
            conditions.add(OrCondition(lastTwo))
        }
        return this
    }
    
    fun not(): ConditionBuilder {
        if (conditions.isNotEmpty()) {
            val last = conditions.removeAt(conditions.size - 1)
            conditions.add(NotCondition(last))
        }
        return this
    }
    
    fun build(): AdvancedCondition? {
        return conditions.firstOrNull()
    }
}

// Extension function for easy condition building
fun condition(block: ConditionBuilder.() -> Unit): AdvancedCondition? {
    return ConditionBuilder().apply(block).build()
}