package com.clickai.macroapp.macro.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent
import android.graphics.Path
import android.util.Log

class MacroAccessibilityService : AccessibilityService() {
    companion object {
        var instance: MacroAccessibilityService? = null
        var recorder: MacroRecorder? = null
        var recording: Boolean = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (recorder == null) recorder = MacroRecorder()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // For demo: record tap events (TYPE_TOUCH_INTERACTION_START)
        if (recording && event != null && event.eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
            val node = event.source ?: return
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            recorder?.recordTap(x, y)
            Log.d("MacroService", "Recorded tap at $x,$y")
        }
    }

    override fun onInterrupt() {
        // Handle service interruption
    }

    fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, null, null)
    }
}