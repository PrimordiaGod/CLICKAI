package com.mycompany.autoclicker.engine

import android.graphics.Point
import android.util.Log
import com.mycompany.autoclicker.macro.*
import kotlinx.coroutines.*
import kotlin.math.max

/**
 * Advanced multi-target click manager for coordinating complex clicking patterns
 */
class MultiTargetClickManager(
    private val tapInterface: TapInterface,
    private val randomizationEngine: RandomizationEngine
) {
    
    private val tag = "MultiTargetClickManager"
    private var currentJob: Job? = null
    private val executionStats = mutableMapOf<String, ActionStats>()
    
    /**
     * Execute advanced action with full feature support
     */
    suspend fun executeAdvancedAction(action: AdvancedAction, screenWidth: Int, screenHeight: Int): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            
            when (action) {
                is AdvancedAction.MultiClick -> executeMultiClick(action, screenWidth, screenHeight)
                is AdvancedAction.SynchronousClick -> executeSynchronousClick(action)
                is AdvancedAction.SequentialClick -> executeSequentialClick(action)
                is AdvancedAction.EdgeClick -> executeEdgeClick(action, screenWidth, screenHeight)
                is AdvancedAction.SmartClick -> executeSmartClick(action)
                is AdvancedAction.ConditionalClick -> executeConditionalClick(action, screenWidth, screenHeight)
                is AdvancedAction.TimedAction -> executeTimedAction(action, screenWidth, screenHeight)
            }
            
            val executionTime = System.currentTimeMillis() - startTime
            updateExecutionStats(action::class.java.simpleName, executionTime, true)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to execute advanced action: ${action::class.java.simpleName}", e)
            updateExecutionStats(action::class.java.simpleName, 0L, false)
            false
        }
    }
    
    /**
     * Execute multi-click action with different modes
     */
    private suspend fun executeMultiClick(action: AdvancedAction.MultiClick, screenWidth: Int, screenHeight: Int) {
        val randomizedPoints = action.randomizationConfig?.let { config ->
            action.points.map { randomizationEngine.randomizePosition(it, config) }
        } ?: action.points
        
        val baseDelay = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeDelay(action.delayMs, config)
        } ?: action.delayMs
        
        delay(baseDelay)
        
        when (action.mode) {
            ClickMode.SYNCHRONOUS -> {
                // Execute all clicks simultaneously
                val jobs = randomizedPoints.map { point ->
                    async {
                        tapInterface.tap(point.x, point.y)
                    }
                }
                jobs.awaitAll()
            }
            
            ClickMode.SEQUENTIAL -> {
                // Execute clicks in sequence
                randomizedPoints.forEach { point ->
                    tapInterface.tap(point.x, point.y)
                    delay(50) // Small delay between sequential clicks
                }
            }
            
            ClickMode.COMBINED -> {
                // Combine with swipes and complex gestures
                executeCombinedGestures(randomizedPoints, action.randomizationConfig)
            }
            
            ClickMode.ADAPTIVE -> {
                // Adapt based on screen response
                executeAdaptiveClicks(randomizedPoints, action.randomizationConfig)
            }
        }
    }
    
    /**
     * Execute synchronous clicking on multiple targets
     */
    private suspend fun executeSynchronousClick(action: AdvancedAction.SynchronousClick) {
        val randomizedTargets = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeMultiTouch(action.targets, config)
        } ?: action.targets.map { target ->
            RandomizedClickTarget(
                originalTarget = target,
                randomizedPoint = target.point,
                delayMs = 0L,
                pressureLevel = target.pressureLevel,
                preClickDelay = 0L
            )
        }
        
        val baseDelay = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeDelay(action.delayMs, config)
        } ?: action.delayMs
        
        delay(baseDelay)
        
        // Execute all clicks simultaneously with individual delays
        val jobs = randomizedTargets.map { target ->
            async {
                delay(target.preClickDelay)
                when (target.originalTarget.clickType) {
                    ClickType.SINGLE -> tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                    ClickType.DOUBLE -> {
                        tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                        delay(100)
                        tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                    }
                    ClickType.TRIPLE -> {
                        repeat(3) { i ->
                            tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                            if (i < 2) delay(100)
                        }
                    }
                    ClickType.LONG_PRESS -> {
                        // Simulate long press with hold duration
                        tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                        delay(target.originalTarget.holdDurationMs)
                    }
                    ClickType.CUSTOM -> {
                        // Custom implementation based on target configuration
                        executeCustomClick(target)
                    }
                }
            }
        }
        
        jobs.awaitAll()
    }
    
    /**
     * Execute sequential clicking with individual loop counts
     */
    private suspend fun executeSequentialClick(action: AdvancedAction.SequentialClick) {
        val randomizedTargets = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeMultiTouch(action.targets, config)
        } ?: action.targets.map { target ->
            RandomizedClickTarget(
                originalTarget = target,
                randomizedPoint = target.point,
                delayMs = 0L,
                pressureLevel = target.pressureLevel,
                preClickDelay = 0L
            )
        }
        
        val baseDelay = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeDelay(action.delayMs, config)
        } ?: action.delayMs
        
        delay(baseDelay)
        
        // Execute targets sequentially with individual loop counts
        randomizedTargets.forEachIndexed { index, target ->
            val loopCount = action.loopCounts[index] ?: 1
            
            repeat(loopCount) {
                delay(target.preClickDelay)
                tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
                
                // Add delay between loops
                if (it < loopCount - 1) {
                    delay(target.delayMs)
                }
            }
            
            // Add delay between different targets
            if (index < randomizedTargets.size - 1) {
                delay(100)
            }
        }
    }
    
    /**
     * Execute edge clicking with screen boundary support
     */
    private suspend fun executeEdgeClick(action: AdvancedAction.EdgeClick, screenWidth: Int, screenHeight: Int) {
        val randomizedPoint = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeEdgeClick(action.edge, action.offset, screenWidth, screenHeight, config)
        } ?: when (action.edge) {
            ScreenEdge.TOP -> Point(screenWidth / 2, (screenHeight * action.offset).toInt())
            ScreenEdge.BOTTOM -> Point(screenWidth / 2, screenHeight - (screenHeight * action.offset).toInt())
            ScreenEdge.LEFT -> Point((screenWidth * action.offset).toInt(), screenHeight / 2)
            ScreenEdge.RIGHT -> Point(screenWidth - (screenWidth * action.offset).toInt(), screenHeight / 2)
            ScreenEdge.TOP_LEFT -> Point((screenWidth * action.offset).toInt(), (screenHeight * action.offset).toInt())
            ScreenEdge.TOP_RIGHT -> Point(screenWidth - (screenWidth * action.offset).toInt(), (screenHeight * action.offset).toInt())
            ScreenEdge.BOTTOM_LEFT -> Point((screenWidth * action.offset).toInt(), screenHeight - (screenHeight * action.offset).toInt())
            ScreenEdge.BOTTOM_RIGHT -> Point(screenWidth - (screenWidth * action.offset).toInt(), screenHeight - (screenHeight * action.offset).toInt())
        }
        
        val baseDelay = action.randomizationConfig?.let { config ->
            randomizationEngine.randomizeDelay(action.delayMs, config)
        } ?: action.delayMs
        
        delay(baseDelay)
        tapInterface.tap(randomizedPoint.x, randomizedPoint.y)
    }
    
    /**
     * Execute smart click with advanced humanization
     */
    private suspend fun executeSmartClick(action: AdvancedAction.SmartClick) {
        val randomizationConfig = action.randomizationConfig ?: RandomizationConfig(
            humanizationLevel = action.humanizationLevel,
            enablePositionRandomization = action.humanizationLevel != HumanizationLevel.NONE,
            positionVarianceRadius = when (action.humanizationLevel) {
                HumanizationLevel.NONE -> 0f
                HumanizationLevel.LOW -> 2f
                HumanizationLevel.MEDIUM -> 5f
                HumanizationLevel.HIGH -> 10f
                HumanizationLevel.EXTREME -> 20f
            }
        )
        
        val humanizedClick = randomizationEngine.humanizeClick(action.point, randomizationConfig)
        val randomizedDelay = randomizationEngine.randomizeDelay(action.delayMs, randomizationConfig)
        
        delay(randomizedDelay)
        delay(humanizedClick.preClickDelay)
        
        // Execute click with humanized characteristics
        tapInterface.tap(humanizedClick.point.x, humanizedClick.point.y)
    }
    
    /**
     * Execute conditional click based on advanced conditions
     */
    private suspend fun executeConditionalClick(action: AdvancedAction.ConditionalClick, screenWidth: Int, screenHeight: Int) {
        // Note: This would need integration with the condition evaluation system
        // For now, we'll execute the actions directly
        for (subAction in action.actions) {
            executeAdvancedAction(subAction, screenWidth, screenHeight)
        }
    }
    
    /**
     * Execute timed action with scheduling
     */
    private suspend fun executeTimedAction(action: AdvancedAction.TimedAction, screenWidth: Int, screenHeight: Int) {
        val scheduleConfig = action.scheduleConfig
        
        // Handle delayed start
        if (scheduleConfig.delayedStartMs > 0) {
            delay(scheduleConfig.delayedStartMs)
        }
        
        // Execute the wrapped action
        executeAdvancedAction(action.action, screenWidth, screenHeight)
    }
    
    /**
     * Execute combined gestures mixing clicks and swipes
     */
    private suspend fun executeCombinedGestures(points: List<Point>, config: RandomizationConfig?) {
        if (points.size < 2) {
            // Single click
            tapInterface.tap(points[0].x, points[0].y)
            return
        }
        
        // Create complex gesture pattern
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            
            // Alternate between clicks and swipes
            if (i % 2 == 0) {
                tapInterface.tap(start.x, start.y)
                delay(100)
            } else {
                tapInterface.swipe(start.x, start.y, end.x, end.y, 200)
                delay(200)
            }
        }
        
        // Final click
        tapInterface.tap(points.last().x, points.last().y)
    }
    
    /**
     * Execute adaptive clicks that respond to screen conditions
     */
    private suspend fun executeAdaptiveClicks(points: List<Point>, config: RandomizationConfig?) {
        for (point in points) {
            val adaptiveDelay = when (config?.humanizationLevel) {
                HumanizationLevel.NONE -> 50L
                HumanizationLevel.LOW -> 75L
                HumanizationLevel.MEDIUM -> 100L
                HumanizationLevel.HIGH -> 150L
                HumanizationLevel.EXTREME -> 200L
                null -> 50L
            }
            
            tapInterface.tap(point.x, point.y)
            delay(adaptiveDelay)
        }
    }
    
    /**
     * Execute custom click implementation
     */
    private suspend fun executeCustomClick(target: RandomizedClickTarget) {
        // Custom implementation based on target configuration
        // This can be extended for specific use cases
        tapInterface.tap(target.randomizedPoint.x, target.randomizedPoint.y)
    }
    
    /**
     * Cancel all running operations
     */
    fun cancelAll() {
        currentJob?.cancel()
        currentJob = null
    }
    
    /**
     * Update execution statistics
     */
    private fun updateExecutionStats(actionType: String, executionTime: Long, success: Boolean) {
        val stats = executionStats.getOrPut(actionType) { ActionStats() }
        
        executionStats[actionType] = stats.copy(
            totalExecutions = stats.totalExecutions + 1,
            successRate = if (success) {
                (stats.successRate * stats.totalExecutions + 1.0f) / (stats.totalExecutions + 1)
            } else {
                (stats.successRate * stats.totalExecutions) / (stats.totalExecutions + 1)
            },
            averageExecutionTime = (stats.averageExecutionTime * stats.totalExecutions + executionTime) / (stats.totalExecutions + 1),
            lastExecutionTime = System.currentTimeMillis(),
            errorCount = if (success) stats.errorCount else stats.errorCount + 1
        )
    }
    
    /**
     * Get execution statistics
     */
    fun getExecutionStats(): Map<String, ActionStats> = executionStats.toMap()
    
    /**
     * Execute burst clicking pattern
     */
    suspend fun executeBurstClick(
        point: Point,
        burstCount: Int,
        baseInterval: Long,
        randomizationConfig: RandomizationConfig?
    ) {
        val intervals = randomizationConfig?.let { config ->
            randomizationEngine.generateBurstIntervals(baseInterval, burstCount, config)
        } ?: List(burstCount) { baseInterval }
        
        repeat(burstCount) { i ->
            val randomizedPoint = randomizationConfig?.let { config ->
                randomizationEngine.randomizePosition(point, config)
            } ?: point
            
            tapInterface.tap(randomizedPoint.x, randomizedPoint.y)
            
            if (i < burstCount - 1) {
                delay(intervals[i])
            }
        }
    }
    
    /**
     * Execute natural movement pattern
     */
    suspend fun executeNaturalMovement(
        start: Point,
        end: Point,
        randomizationConfig: RandomizationConfig?
    ) {
        val curve = randomizationConfig?.let { config ->
            randomizationEngine.generateNaturalCurve(start, end, config)
        } ?: listOf(start, end)
        
        if (curve.size <= 2) {
            // Direct movement
            tapInterface.swipe(start.x, start.y, end.x, end.y, 500)
        } else {
            // Natural curve movement
            for (i in 0 until curve.size - 1) {
                val currentPoint = curve[i]
                val nextPoint = curve[i + 1]
                
                tapInterface.swipe(currentPoint.x, currentPoint.y, nextPoint.x, nextPoint.y, 50)
                delay(20)
            }
        }
    }
}