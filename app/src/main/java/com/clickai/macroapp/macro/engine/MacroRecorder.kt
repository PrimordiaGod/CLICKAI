package com.clickai.macroapp.macro.engine

import android.view.accessibility.AccessibilityEvent

// Data class for a macro action
sealed class MacroAction {
    data class Tap(val x: Float, val y: Float, val delay: Long = 0) : MacroAction()
    data class Swipe(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val duration: Long = 300, val delay: Long = 0) : MacroAction()
    data class Wait(val duration: Long) : MacroAction()
}

class MacroRecorder {
    private val actions = mutableListOf<MacroAction>()
    private var lastEventTime: Long = 0

    fun recordTap(x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val delay = if (lastEventTime == 0L) 0 else now - lastEventTime
        actions.add(MacroAction.Tap(x, y, delay))
        lastEventTime = now
    }

    fun recordSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300) {
        val now = System.currentTimeMillis()
        val delay = if (lastEventTime == 0L) 0 else now - lastEventTime
        actions.add(MacroAction.Swipe(x1, y1, x2, y2, duration, delay))
        lastEventTime = now
    }

    fun recordWait(duration: Long) {
        actions.add(MacroAction.Wait(duration))
    }

    fun getActions(): List<MacroAction> = actions.toList()
    fun clear() {
        actions.clear()
        lastEventTime = 0
    }
}