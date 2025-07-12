package com.mycompany.autoclicker.engine

import android.graphics.Point
import android.graphics.PointF
import com.mycompany.autoclicker.macro.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced randomization engine for anti-detection and human-like behavior
 */
class RandomizationEngine {
    
    private val random = Random.Default
    private val humanizationPatterns = mutableMapOf<HumanizationLevel, HumanizationPattern>()
    private val adaptiveDelayTracker = AdaptiveDelayTracker()
    
    init {
        initializeHumanizationPatterns()
    }
    
    /**
     * Randomize timing based on configuration
     */
    fun randomizeDelay(baseDelayMs: Long, config: RandomizationConfig): Long {
        if (!config.enableTimingRandomization) return baseDelayMs
        
        val varianceMs = when {
            config.timingVarianceMs > 0 -> config.timingVarianceMs
            config.timingVariancePercent > 0 -> (baseDelayMs * config.timingVariancePercent / 100).toLong()
            else -> 0L
        }
        
        if (varianceMs == 0L) return baseDelayMs
        
        val randomVariance = random.nextLong(-varianceMs, varianceMs + 1)
        val randomizedDelay = baseDelayMs + randomVariance
        
        return if (config.enableAdaptiveDelays) {
            adaptiveDelayTracker.adjustDelay(randomizedDelay, config.humanizationLevel)
        } else {
            randomizedDelay
        }.coerceAtLeast(1L) // Ensure minimum delay
    }
    
    /**
     * Randomize position based on configuration
     */
    fun randomizePosition(basePoint: Point, config: RandomizationConfig): Point {
        if (!config.enablePositionRandomization || config.positionVarianceRadius <= 0f) {
            return basePoint
        }
        
        val angle = random.nextFloat() * 2 * PI
        val radius = random.nextFloat() * config.positionVarianceRadius
        
        val offsetX = (radius * cos(angle)).toInt()
        val offsetY = (radius * sin(angle)).toInt()
        
        return Point(
            basePoint.x + offsetX,
            basePoint.y + offsetY
        )
    }
    
    /**
     * Apply humanization to click pattern
     */
    fun humanizeClick(point: Point, config: RandomizationConfig): HumanizedClick {
        val pattern = humanizationPatterns[config.humanizationLevel] ?: return HumanizedClick(point, 0L, 1.0f)
        
        val humanizedPoint = if (config.enablePositionRandomization) {
            addMicroMovements(point, pattern)
        } else {
            point
        }
        
        val preClickDelay = generatePreClickDelay(pattern)
        val pressureVariation = if (config.pressureVariation) {
            generatePressureVariation(pattern)
        } else {
            1.0f
        }
        
        return HumanizedClick(humanizedPoint, preClickDelay, pressureVariation)
    }
    
    /**
     * Generate natural movement curve between two points
     */
    fun generateNaturalCurve(start: Point, end: Point, config: RandomizationConfig): List<Point> {
        if (!config.naturalCurveEnabled) return listOf(start, end)
        
        val distance = sqrt((end.x - start.x).toDouble().pow(2) + (end.y - start.y).toDouble().pow(2))
        val steps = (distance / 10).toInt().coerceAtLeast(2).coerceAtMost(20)
        
        val controlPoints = generateBezierControlPoints(start, end, config.humanizationLevel)
        return generateBezierCurve(start, controlPoints, end, steps)
    }
    
    /**
     * Generate random intervals for burst clicking
     */
    fun generateBurstIntervals(baseInterval: Long, burstCount: Int, config: RandomizationConfig): List<Long> {
        val intervals = mutableListOf<Long>()
        
        for (i in 0 until burstCount) {
            val variance = when (config.humanizationLevel) {
                HumanizationLevel.NONE -> 0L
                HumanizationLevel.LOW -> baseInterval * 0.1
                HumanizationLevel.MEDIUM -> baseInterval * 0.2
                HumanizationLevel.HIGH -> baseInterval * 0.3
                HumanizationLevel.EXTREME -> baseInterval * 0.5
            }
            
            val randomizedInterval = baseInterval + random.nextLong((-variance).toLong(), variance.toLong() + 1)
            intervals.add(randomizedInterval.coerceAtLeast(1L))
        }
        
        return intervals
    }
    
    /**
     * Apply edge clicking randomization
     */
    fun randomizeEdgeClick(edge: ScreenEdge, offset: Float, screenWidth: Int, screenHeight: Int, config: RandomizationConfig): Point {
        val basePoint = when (edge) {
            ScreenEdge.TOP -> Point(screenWidth / 2, (screenHeight * offset).toInt())
            ScreenEdge.BOTTOM -> Point(screenWidth / 2, screenHeight - (screenHeight * offset).toInt())
            ScreenEdge.LEFT -> Point((screenWidth * offset).toInt(), screenHeight / 2)
            ScreenEdge.RIGHT -> Point(screenWidth - (screenWidth * offset).toInt(), screenHeight / 2)
            ScreenEdge.TOP_LEFT -> Point((screenWidth * offset).toInt(), (screenHeight * offset).toInt())
            ScreenEdge.TOP_RIGHT -> Point(screenWidth - (screenWidth * offset).toInt(), (screenHeight * offset).toInt())
            ScreenEdge.BOTTOM_LEFT -> Point((screenWidth * offset).toInt(), screenHeight - (screenHeight * offset).toInt())
            ScreenEdge.BOTTOM_RIGHT -> Point(screenWidth - (screenWidth * offset).toInt(), screenHeight - (screenHeight * offset).toInt())
        }
        
        return randomizePosition(basePoint, config)
    }
    
    /**
     * Generate randomized multi-touch pattern
     */
    fun randomizeMultiTouch(targets: List<ClickTarget>, config: RandomizationConfig): List<RandomizedClickTarget> {
        return targets.mapIndexed { index, target ->
            val randomizedPoint = randomizePosition(target.point, config)
            val randomizedDelay = randomizeDelay(50L * index, config) // Stagger multi-touch
            val humanizedClick = humanizeClick(randomizedPoint, config)
            
            RandomizedClickTarget(
                originalTarget = target,
                randomizedPoint = humanizedClick.point,
                delayMs = randomizedDelay,
                pressureLevel = humanizedClick.pressureVariation,
                preClickDelay = humanizedClick.preClickDelay
            )
        }
    }
    
    private fun initializeHumanizationPatterns() {
        humanizationPatterns[HumanizationLevel.NONE] = HumanizationPattern(
            microMovementRange = 0f,
            preClickDelayRange = 0L..0L,
            pressureVariationRange = 1.0f..1.0f,
            curveComplexity = 0
        )
        
        humanizationPatterns[HumanizationLevel.LOW] = HumanizationPattern(
            microMovementRange = 2f,
            preClickDelayRange = 0L..20L,
            pressureVariationRange = 0.9f..1.1f,
            curveComplexity = 1
        )
        
        humanizationPatterns[HumanizationLevel.MEDIUM] = HumanizationPattern(
            microMovementRange = 5f,
            preClickDelayRange = 10L..50L,
            pressureVariationRange = 0.8f..1.2f,
            curveComplexity = 2
        )
        
        humanizationPatterns[HumanizationLevel.HIGH] = HumanizationPattern(
            microMovementRange = 10f,
            preClickDelayRange = 20L..100L,
            pressureVariationRange = 0.7f..1.3f,
            curveComplexity = 3
        )
        
        humanizationPatterns[HumanizationLevel.EXTREME] = HumanizationPattern(
            microMovementRange = 20f,
            preClickDelayRange = 50L..200L,
            pressureVariationRange = 0.5f..1.5f,
            curveComplexity = 4
        )
    }
    
    private fun addMicroMovements(point: Point, pattern: HumanizationPattern): Point {
        val offsetX = (random.nextFloat() - 0.5f) * 2 * pattern.microMovementRange
        val offsetY = (random.nextFloat() - 0.5f) * 2 * pattern.microMovementRange
        
        return Point(
            point.x + offsetX.toInt(),
            point.y + offsetY.toInt()
        )
    }
    
    private fun generatePreClickDelay(pattern: HumanizationPattern): Long {
        return random.nextLong(pattern.preClickDelayRange.first, pattern.preClickDelayRange.last + 1)
    }
    
    private fun generatePressureVariation(pattern: HumanizationPattern): Float {
        return random.nextFloat() * (pattern.pressureVariationRange.endInclusive - pattern.pressureVariationRange.start) + pattern.pressureVariationRange.start
    }
    
    private fun generateBezierControlPoints(start: Point, end: Point, humanizationLevel: HumanizationLevel): List<Point> {
        val controlPointCount = when (humanizationLevel) {
            HumanizationLevel.NONE -> 0
            HumanizationLevel.LOW -> 1
            HumanizationLevel.MEDIUM -> 2
            HumanizationLevel.HIGH -> 3
            HumanizationLevel.EXTREME -> 4
        }
        
        val controlPoints = mutableListOf<Point>()
        val distance = sqrt((end.x - start.x).toDouble().pow(2) + (end.y - start.y).toDouble().pow(2))
        val variance = distance * 0.1
        
        for (i in 1..controlPointCount) {
            val t = i.toFloat() / (controlPointCount + 1)
            val baseX = start.x + (end.x - start.x) * t
            val baseY = start.y + (end.y - start.y) * t
            
            val offsetX = (random.nextFloat() - 0.5f) * 2 * variance
            val offsetY = (random.nextFloat() - 0.5f) * 2 * variance
            
            controlPoints.add(Point(
                (baseX + offsetX).toInt(),
                (baseY + offsetY).toInt()
            ))
        }
        
        return controlPoints
    }
    
    private fun generateBezierCurve(start: Point, controlPoints: List<Point>, end: Point, steps: Int): List<Point> {
        val allPoints = listOf(start) + controlPoints + listOf(end)
        val curve = mutableListOf<Point>()
        
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val point = evaluateBezier(allPoints, t)
            curve.add(point)
        }
        
        return curve
    }
    
    private fun evaluateBezier(points: List<Point>, t: Float): Point {
        if (points.size == 1) return points[0]
        
        val newPoints = mutableListOf<PointF>()
        for (i in 0 until points.size - 1) {
            val x = points[i].x * (1 - t) + points[i + 1].x * t
            val y = points[i].y * (1 - t) + points[i + 1].y * t
            newPoints.add(PointF(x, y))
        }
        
        return evaluateBezier(newPoints.map { Point(it.x.toInt(), it.y.toInt()) }, t)
    }
}

/**
 * Data classes for randomization
 */
data class HumanizedClick(
    val point: Point,
    val preClickDelay: Long,
    val pressureVariation: Float
)

data class RandomizedClickTarget(
    val originalTarget: ClickTarget,
    val randomizedPoint: Point,
    val delayMs: Long,
    val pressureLevel: Float,
    val preClickDelay: Long
)

data class HumanizationPattern(
    val microMovementRange: Float,
    val preClickDelayRange: LongRange,
    val pressureVariationRange: ClosedFloatingPointRange<Float>,
    val curveComplexity: Int
)

/**
 * Adaptive delay tracker for learning user patterns
 */
class AdaptiveDelayTracker {
    private val delayHistory = mutableListOf<Long>()
    private val maxHistorySize = 100
    
    fun adjustDelay(baseDelay: Long, humanizationLevel: HumanizationLevel): Long {
        // Learn from previous delays
        if (delayHistory.size >= maxHistorySize) {
            delayHistory.removeFirst()
        }
        
        val adjustedDelay = when (humanizationLevel) {
            HumanizationLevel.NONE -> baseDelay
            else -> {
                val avgDelay = if (delayHistory.isNotEmpty()) {
                    delayHistory.average().toLong()
                } else {
                    baseDelay
                }
                
                // Blend with historical average
                val blendFactor = when (humanizationLevel) {
                    HumanizationLevel.LOW -> 0.1f
                    HumanizationLevel.MEDIUM -> 0.2f
                    HumanizationLevel.HIGH -> 0.3f
                    HumanizationLevel.EXTREME -> 0.4f
                    else -> 0f
                }
                
                (baseDelay * (1 - blendFactor) + avgDelay * blendFactor).toLong()
            }
        }
        
        delayHistory.add(adjustedDelay)
        return adjustedDelay
    }
}