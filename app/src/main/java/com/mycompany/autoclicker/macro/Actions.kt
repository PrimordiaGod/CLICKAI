package com.mycompany.autoclicker.macro

sealed class Action {
    data class Click(
        val x: Int,
        val y: Int,
        var delayMs: Long = 0L,
        var jitterPx: Int = 0,
        var delayVarianceMs: Long = 0L
    ) : Action()

    data class Swipe(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val durationMs: Int = 100,
        var delayMs: Long = 0L,
        var jitterPx: Int = 0,
        var delayVarianceMs: Long = 0L
    ) : Action()
    data class Wait(val millis: Long) : Action()
    data class InputText(val text: String) : Action()
    data class ClickNorm(val n: util.ScreenAdapter.Normalised, var delayMs: Long = 0L) : Action()
    data class SwipeNorm(val start: util.ScreenAdapter.Normalised, val end: util.ScreenAdapter.Normalised, val durationMs: Int = 100, var delayMs: Long = 0L) : Action()
}