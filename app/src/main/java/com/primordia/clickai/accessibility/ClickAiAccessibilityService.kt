package com.primordia.clickai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.primordia.clickai.ui.ServiceLocator

class ClickAiAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ClickAI", "Accessibility Service connected")
        ServiceLocator.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Observe events if needed
    }

    override fun onInterrupt() {
        Log.w("ClickAI", "Accessibility Service interrupted")
    }

    fun performTap(x: Float, y: Float, onCompleted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onCompleted?.let { mainHandler.post(it) }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onCompleted?.let { mainHandler.post(it) }
            }
        }, null)
    }

    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300, onCompleted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onCompleted?.let { mainHandler.post(it) }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onCompleted?.let { mainHandler.post(it) }
            }
        }, null)
    }

    fun findNodesByText(text: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByText(text)?.toList().orEmpty()
    }

    fun nodeBounds(node: AccessibilityNodeInfo): Rect {
        val r = Rect()
        node.getBoundsInScreen(r)
        return r
    }
}