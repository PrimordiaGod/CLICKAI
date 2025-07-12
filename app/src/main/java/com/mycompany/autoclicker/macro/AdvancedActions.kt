package com.mycompany.autoclicker.macro

import android.graphics.Point
import android.graphics.PointF
import java.time.LocalTime
import java.time.DayOfWeek

/**
 * Enhanced action system supporting multi-target clicking, randomization, and advanced features
 */
sealed class AdvancedAction {
    abstract val delayMs: Long
    
    data class MultiClick(
        val points: List<Point>, 
        val mode: ClickMode, 
        override val delayMs: Long,
        val randomizationConfig: RandomizationConfig? = null
    ) : AdvancedAction()
    
    data class SynchronousClick(
        val targets: List<ClickTarget>,
        override val delayMs: Long,
        val randomizationConfig: RandomizationConfig? = null
    ) : AdvancedAction()
    
    data class SequentialClick(
        val targets: List<ClickTarget>, 
        val loopCounts: Map<Int, Int>,
        override val delayMs: Long,
        val randomizationConfig: RandomizationConfig? = null
    ) : AdvancedAction()
    
    data class EdgeClick(
        val edge: ScreenEdge, 
        val offset: Float,
        override val delayMs: Long,
        val randomizationConfig: RandomizationConfig? = null
    ) : AdvancedAction()
    
    data class SmartClick(
        val point: Point,
        val humanizationLevel: HumanizationLevel,
        override val delayMs: Long,
        val randomizationConfig: RandomizationConfig? = null
    ) : AdvancedAction()
    
    data class ConditionalClick(
        val condition: AdvancedCondition,
        val actions: List<AdvancedAction>,
        override val delayMs: Long = 0L
    ) : AdvancedAction()
    
    data class TimedAction(
        val action: AdvancedAction,
        val scheduleConfig: ScheduleConfig,
        override val delayMs: Long = 0L
    ) : AdvancedAction()
}

/**
 * Click target with advanced properties
 */
data class ClickTarget(
    val point: Point,
    val clickType: ClickType = ClickType.SINGLE,
    val button: MouseButton = MouseButton.LEFT,
    val pressureLevel: Float = 1.0f,
    val holdDurationMs: Long = 0L,
    val area: ClickArea? = null
)

/**
 * Click area for position randomization
 */
data class ClickArea(
    val center: Point,
    val radiusX: Float,
    val radiusY: Float,
    val shape: AreaShape = AreaShape.CIRCLE
)

/**
 * Randomization configuration for anti-detection
 */
data class RandomizationConfig(
    val enableTimingRandomization: Boolean = false,
    val timingVariancePercent: Float = 0f,
    val timingVarianceMs: Long = 0L,
    val enablePositionRandomization: Boolean = false,
    val positionVarianceRadius: Float = 0f,
    val enableAdaptiveDelays: Boolean = false,
    val humanizationLevel: HumanizationLevel = HumanizationLevel.NONE,
    val naturalCurveEnabled: Boolean = false,
    val pressureVariation: Boolean = false
)

/**
 * Scheduling configuration
 */
data class ScheduleConfig(
    val delayedStartMs: Long = 0L,
    val autoLaunchApps: List<String> = emptyList(),
    val scheduledSessions: List<ScheduledSession> = emptyList(),
    val conditionTriggers: List<TriggerCondition> = emptyList(),
    val smartPauseEnabled: Boolean = true,
    val maxDurationMs: Long = 0L, // 0 = unlimited
    val maxClickCount: Int = 0 // 0 = unlimited
)

/**
 * Scheduled session
 */
data class ScheduledSession(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val macroName: String,
    val priority: Int = 0
)

/**
 * Trigger condition for automatic activation
 */
sealed class TriggerCondition {
    data class AppLaunched(val packageName: String) : TriggerCondition()
    data class ImageDetected(val templateName: String, val confidence: Float) : TriggerCondition()
    data class ColorDetected(val color: Int, val tolerance: Float) : TriggerCondition()
    data class TextDetected(val text: String, val caseSensitive: Boolean) : TriggerCondition()
    data class TimeBasedTrigger(val time: LocalTime) : TriggerCondition()
    data class BatteryLevel(val threshold: Int, val operator: ComparisonOperator) : TriggerCondition()
}

/**
 * Advanced conditions for complex logic
 */
sealed class AdvancedCondition {
    data class MultipleImages(val templates: List<String>, val allRequired: Boolean) : AdvancedCondition()
    data class ColorRange(val colorMin: Int, val colorMax: Int, val area: ClickArea) : AdvancedCondition()
    data class TextPresence(val texts: List<String>, val anyMatch: Boolean) : AdvancedCondition()
    data class TimeWindow(val startTime: LocalTime, val endTime: LocalTime) : AdvancedCondition()
    data class AppState(val packageName: String, val isRunning: Boolean) : AdvancedCondition()
    data class ScreenBrightness(val threshold: Float, val operator: ComparisonOperator) : AdvancedCondition()
    data class LogicalCondition(val conditions: List<AdvancedCondition>, val operator: LogicalOperator) : AdvancedCondition()
}

/**
 * Enums for various configuration options
 */
enum class ClickMode { SYNCHRONOUS, SEQUENTIAL, COMBINED, ADAPTIVE }
enum class ScreenEdge { TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
enum class ClickType { SINGLE, DOUBLE, TRIPLE, LONG_PRESS, CUSTOM }
enum class MouseButton { LEFT, RIGHT, MIDDLE }
enum class AreaShape { CIRCLE, RECTANGLE, ELLIPSE }
enum class HumanizationLevel { NONE, LOW, MEDIUM, HIGH, EXTREME }
enum class ComparisonOperator { EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL }
enum class LogicalOperator { AND, OR, NOT, XOR }

/**
 * Statistics tracking for performance and optimization
 */
data class ActionStats(
    val totalExecutions: Long = 0L,
    val successRate: Float = 0f,
    val averageExecutionTime: Long = 0L,
    val lastExecutionTime: Long = 0L,
    val errorCount: Long = 0L,
    val detectionAccuracy: Float = 0f
)

/**
 * Performance configuration for optimization
 */
data class PerformanceConfig(
    val frameSkipRate: Int = 1, // Process every Nth frame
    val regionOfInterest: ClickArea? = null, // Limit detection to specific area
    val gpuAccelerationEnabled: Boolean = true,
    val memoryOptimizationEnabled: Boolean = true,
    val batteryOptimizationEnabled: Boolean = true,
    val maxConcurrentOperations: Int = 4
)

/**
 * UI configuration for floating controls
 */
data class UIConfig(
    val skinTheme: SkinTheme = SkinTheme.DEFAULT,
    val transparency: Float = 1.0f,
    val orientation: Orientation = Orientation.HORIZONTAL,
    val position: PointF = PointF(0f, 0f),
    val isMinimized: Boolean = false,
    val showClickIndicators: Boolean = true,
    val showStatistics: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val enableSoundFeedback: Boolean = false
)

enum class SkinTheme { DEFAULT, NEON, MINIMAL, GAMER, PROFESSIONAL, CUSTOM }
enum class Orientation { HORIZONTAL, VERTICAL }

/**
 * Configuration profile for saving/loading setups
 */
data class ConfigProfile(
    val name: String,
    val description: String,
    val actions: List<AdvancedAction>,
    val randomizationConfig: RandomizationConfig,
    val scheduleConfig: ScheduleConfig,
    val performanceConfig: PerformanceConfig,
    val uiConfig: UIConfig,
    val createdAt: Long = System.currentTimeMillis(),
    val version: String = "1.0",
    val tags: List<String> = emptyList()
)