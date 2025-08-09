package com.primordia.clickai.engine

sealed interface Step { val type: String }

data class Click(val x: Float, val y: Float) : Step { override val type = "Click" }

data class Swipe(
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val durationMs: Long
) : Step { override val type = "Swipe" }

data class Wait(val durationMs: Long) : Step { override val type = "Wait" }

data class OcrWait(val text: String, val timeoutMs: Long, val region: Region? = null) : Step { override val type = "OcrWait" }

data class ImageMatch(
    val templateAsset: String,
    val threshold: Float,
    val timeoutMs: Long,
    val region: Region? = null
) : Step { override val type = "ImageMatch" }

data class Loop(val count: Int, val steps: List<Step>) : Step { override val type = "Loop" }

data class Conditional(
    val predicate: Step,
    val thenSteps: List<Step>,
    val elseSteps: List<Step> = emptyList()
) : Step { override val type = "Conditional" }

data class Region(val left: Int, val top: Int, val right: Int, val bottom: Int)

data class Macro(val version: String = "1.0.0", val name: String, val steps: List<Step>)