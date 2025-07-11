package com.mycompany.autoclicker.macro

sealed class Action {
    data class Click(val x: Int, val y: Int, var delayMs: Long = 0L) : Action()
    data class Swipe(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val durationMs: Int = 100, var delayMs: Long = 0L) : Action()
    data class Wait(val millis: Long) : Action()
    data class InputText(val text: String) : Action()
}