package com.mycompany.autoclicker.tap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mycompany.autoclicker.macro.TapInterface
import com.mycompany.autoclicker.engine.RandomizedClickTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Advanced tap client with multi-touch support, pressure sensitivity, and gesture patterns
 */
class AdvancedTapClient(
    private val context: Context,
    private val accessibilityService: AccessibilityService? = null
) : TapInterface {
    
    private val tag = "AdvancedTapClient"
    private val handler = Handler(Looper.getMainLooper())
    private val fallbackClient = TapClient(context)
    
    // Multi-touch support
    private var maxMultiTouchPoints = 10
    private var currentGestures = mutableListOf<GestureDescription>()
    
    // Statistics tracking
    private var totalTaps = 0L
    private var totalSwipes = 0L
    private var totalGestures = 0L
    private var lastActionTime = 0L
    
    /**
     * Enhanced tap with pressure and timing support
     */
    override fun tap(x: Int, y: Int) {
        performAdvancedTap(x, y, 1.0f, 100L)
    }
    
    /**
     * Enhanced swipe with curved path support
     */
    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        performAdvancedSwipe(x1, y1, x2, y2, durationMs.toLong())
    }
    
    /**
     * Enhanced input text with timing
     */
    override fun inputText(text: String) {
        performAdvancedTextInput(text)
    }
    
    /**
     * Perform advanced tap with pressure sensitivity
     */
    suspend fun performAdvancedTap(x: Int, y: Int, pressure: Float = 1.0f, holdDuration: Long = 100L) {
        withContext(Dispatchers.Main) {
            try {
                if (accessibilityService != null) {
                    val gesture = createTapGesture(x, y, pressure, holdDuration)
                    accessibilityService.dispatchGesture(gesture, null, null)
                } else {
                    // Fallback to basic tap
                    fallbackClient.tap(x, y)
                }
                totalTaps++
                lastActionTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform advanced tap", e)
                fallbackClient.tap(x, y)
            }
        }
    }
    
    /**
     * Perform advanced swipe with curved path
     */
    suspend fun performAdvancedSwipe(
        x1: Int, y1: Int, x2: Int, y2: Int, 
        durationMs: Long, 
        curvePath: List<Point>? = null
    ) {
        withContext(Dispatchers.Main) {
            try {
                if (accessibilityService != null) {
                    val gesture = if (curvePath != null) {
                        createCurvedSwipeGesture(curvePath, durationMs)
                    } else {
                        createSwipeGesture(x1, y1, x2, y2, durationMs)
                    }
                    accessibilityService.dispatchGesture(gesture, null, null)
                } else {
                    // Fallback to basic swipe
                    fallbackClient.swipe(x1, y1, x2, y2, durationMs.toInt())
                }
                totalSwipes++
                lastActionTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform advanced swipe", e)
                fallbackClient.swipe(x1, y1, x2, y2, durationMs.toInt())
            }
        }
    }
    
    /**
     * Perform multi-touch gesture
     */
    suspend fun performMultiTouch(targets: List<RandomizedClickTarget>) {
        withContext(Dispatchers.Main) {
            try {
                if (accessibilityService != null && targets.size <= maxMultiTouchPoints) {
                    val gesture = createMultiTouchGesture(targets)
                    accessibilityService.dispatchGesture(gesture, null, null)
                } else {
                    // Fallback to sequential taps
                    for (target in targets) {
                        delay(target.preClickDelay)
                        performAdvancedTap(
                            target.randomizedPoint.x,
                            target.randomizedPoint.y,
                            target.pressureLevel,
                            target.originalTarget.holdDurationMs
                        )
                        delay(target.delayMs)
                    }
                }
                totalGestures++
                lastActionTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform multi-touch gesture", e)
                // Fallback to sequential taps
                for (target in targets) {
                    fallbackClient.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                    delay(target.delayMs)
                }
            }
        }
    }
    
    /**
     * Perform long press gesture
     */
    suspend fun performLongPress(x: Int, y: Int, durationMs: Long) {
        withContext(Dispatchers.Main) {
            try {
                if (accessibilityService != null) {
                    val gesture = createLongPressGesture(x, y, durationMs)
                    accessibilityService.dispatchGesture(gesture, null, null)
                } else {
                    // Fallback to tap + delay
                    fallbackClient.tap(x, y)
                    delay(durationMs)
                }
                totalTaps++
                lastActionTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform long press", e)
                fallbackClient.tap(x, y)
                delay(durationMs)
            }
        }
    }
    
    /**
     * Perform double tap gesture
     */
    suspend fun performDoubleTap(x: Int, y: Int, delayBetweenTaps: Long = 100L) {
        performAdvancedTap(x, y)
        delay(delayBetweenTaps)
        performAdvancedTap(x, y)
    }
    
    /**
     * Perform triple tap gesture
     */
    suspend fun performTripleTap(x: Int, y: Int, delayBetweenTaps: Long = 100L) {
        repeat(3) { i ->
            performAdvancedTap(x, y)
            if (i < 2) delay(delayBetweenTaps)
        }
    }
    
    /**
     * Perform pinch gesture
     */
    suspend fun performPinch(
        centerX: Int, centerY: Int, 
        startRadius: Float, endRadius: Float, 
        durationMs: Long
    ) {
        withContext(Dispatchers.Main) {
            try {
                if (accessibilityService != null) {
                    val gesture = createPinchGesture(centerX, centerY, startRadius, endRadius, durationMs)
                    accessibilityService.dispatchGesture(gesture, null, null)
                } else {
                    Log.w(tag, "Pinch gesture not supported without accessibility service")
                }
                totalGestures++
                lastActionTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform pinch gesture", e)
            }
        }
    }
    
    /**
     * Perform advanced text input with human-like timing
     */
    private suspend fun performAdvancedTextInput(text: String) {
        withContext(Dispatchers.IO) {
            try {
                for (char in text) {
                    // Add human-like delay between characters
                    val charDelay = when (char) {
                        ' ' -> 50L..150L
                        '.' -> 100L..200L
                        ',' -> 80L..120L
                        else -> 30L..80L
                    }
                    
                    fallbackClient.inputText(char.toString())
                    delay(charDelay.random())
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to perform advanced text input", e)
                fallbackClient.inputText(text)
            }
        }
    }
    
    /**
     * Create tap gesture description
     */
    private fun createTapGesture(x: Int, y: Int, pressure: Float, holdDuration: Long): GestureDescription {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,
            holdDuration,
            false
        )
        
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        return builder.build()
    }
    
    /**
     * Create swipe gesture description
     */
    private fun createSwipeGesture(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long): GestureDescription {
        val path = Path()
        path.moveTo(x1.toFloat(), y1.toFloat())
        path.lineTo(x2.toFloat(), y2.toFloat())
        
        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,
            durationMs,
            false
        )
        
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        return builder.build()
    }
    
    /**
     * Create curved swipe gesture
     */
    private fun createCurvedSwipeGesture(curvePath: List<Point>, durationMs: Long): GestureDescription {
        val path = Path()
        if (curvePath.isNotEmpty()) {
            path.moveTo(curvePath[0].x.toFloat(), curvePath[0].y.toFloat())
            for (i in 1 until curvePath.size) {
                path.lineTo(curvePath[i].x.toFloat(), curvePath[i].y.toFloat())
            }
        }
        
        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,
            durationMs,
            false
        )
        
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        return builder.build()
    }
    
    /**
     * Create multi-touch gesture
     */
    private fun createMultiTouchGesture(targets: List<RandomizedClickTarget>): GestureDescription {
        val builder = GestureDescription.Builder()
        
        targets.forEach { target ->
            val path = Path()
            path.moveTo(target.randomizedPoint.x.toFloat(), target.randomizedPoint.y.toFloat())
            
            val stroke = GestureDescription.StrokeDescription(
                path,
                target.preClickDelay,
                target.originalTarget.holdDurationMs.coerceAtLeast(100L),
                false
            )
            
            builder.addStroke(stroke)
        }
        
        return builder.build()
    }
    
    /**
     * Create long press gesture
     */
    private fun createLongPressGesture(x: Int, y: Int, durationMs: Long): GestureDescription {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        
        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,
            durationMs,
            false
        )
        
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        return builder.build()
    }
    
    /**
     * Create pinch gesture
     */
    private fun createPinchGesture(
        centerX: Int, centerY: Int, 
        startRadius: Float, endRadius: Float, 
        durationMs: Long
    ): GestureDescription {
        val builder = GestureDescription.Builder()
        
        // Create two finger paths for pinch
        val path1 = Path()
        val path2 = Path()
        
        // First finger starts at top and moves based on pinch direction
        val startY1 = centerY - startRadius.toInt()
        val endY1 = centerY - endRadius.toInt()
        path1.moveTo(centerX.toFloat(), startY1.toFloat())
        path1.lineTo(centerX.toFloat(), endY1.toFloat())
        
        // Second finger starts at bottom and moves based on pinch direction
        val startY2 = centerY + startRadius.toInt()
        val endY2 = centerY + endRadius.toInt()
        path2.moveTo(centerX.toFloat(), startY2.toFloat())
        path2.lineTo(centerX.toFloat(), endY2.toFloat())
        
        val stroke1 = GestureDescription.StrokeDescription(path1, 0L, durationMs, false)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0L, durationMs, false)
        
        builder.addStroke(stroke1)
        builder.addStroke(stroke2)
        
        return builder.build()
    }
    
    /**
     * Get tap statistics
     */
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalTaps" to totalTaps,
            "totalSwipes" to totalSwipes,
            "totalGestures" to totalGestures,
            "lastActionTime" to lastActionTime,
            "averageClicksPerSecond" to calculateAverageClicksPerSecond()
        )
    }
    
    /**
     * Calculate average clicks per second
     */
    private fun calculateAverageClicksPerSecond(): Float {
        if (totalTaps == 0L || lastActionTime == 0L) return 0f
        
        val sessionDuration = (System.currentTimeMillis() - lastActionTime) / 1000f
        return if (sessionDuration > 0) totalTaps / sessionDuration else 0f
    }
    
    /**
     * Reset statistics
     */
    fun resetStatistics() {
        totalTaps = 0L
        totalSwipes = 0L
        totalGestures = 0L
        lastActionTime = 0L
    }
    
    /**
     * Check if advanced features are available
     */
    fun isAdvancedFeaturesAvailable(): Boolean {
        return accessibilityService != null
    }
    
    /**
     * Check if multi-touch is supported
     */
    fun isMultiTouchSupported(): Boolean {
        return accessibilityService != null && maxMultiTouchPoints > 1
    }
    
    /**
     * Set maximum multi-touch points
     */
    fun setMaxMultiTouchPoints(max: Int) {
        maxMultiTouchPoints = max.coerceIn(1, 10)
    }
    
    /**
     * Cancel all pending gestures
     */
    fun cancelAllGestures() {
        currentGestures.clear()
    }
    
    /**
     * Perform edge tap with screen boundary detection
     */
    suspend fun performEdgeTap(x: Int, y: Int, screenWidth: Int, screenHeight: Int) {
        // Ensure coordinates are within screen bounds
        val safeX = x.coerceIn(0, screenWidth - 1)
        val safeY = y.coerceIn(0, screenHeight - 1)
        
        // Add slight inward offset for edge coordinates
        val adjustedX = when {
            safeX < 10 -> safeX + 5
            safeX > screenWidth - 10 -> safeX - 5
            else -> safeX
        }
        
        val adjustedY = when {
            safeY < 10 -> safeY + 5
            safeY > screenHeight - 10 -> safeY - 5
            else -> safeY
        }
        
        performAdvancedTap(adjustedX, adjustedY)
    }
    
    /**
     * Perform burst clicking
     */
    suspend fun performBurstClick(x: Int, y: Int, count: Int, intervals: List<Long>) {
        repeat(count) { i ->
            performAdvancedTap(x, y)
            if (i < count - 1 && i < intervals.size) {
                delay(intervals[i])
            }
        }
    }
    
    /**
     * Perform natural movement sequence
     */
    suspend fun performNaturalMovement(path: List<Point>, totalDuration: Long) {
        if (path.size < 2) return
        
        val segmentDuration = totalDuration / (path.size - 1)
        
        for (i in 0 until path.size - 1) {
            val start = path[i]
            val end = path[i + 1]
            
            performAdvancedSwipe(start.x, start.y, end.x, end.y, segmentDuration)
            if (i < path.size - 2) {
                delay(50) // Small delay between segments
            }
        }
    }
}