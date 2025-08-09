package com.primordia.clickai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.*
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.primordia.clickai.ui.ServiceLocator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ClickAiAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ClickAI", "Accessibility Service connected")
        ServiceLocator.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { Log.w("ClickAI", "Accessibility Service interrupted") }

    fun performTap(x: Float, y: Float, onCompleted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) { onCompleted?.let { mainHandler.post(it) } }
            override fun onCancelled(gestureDescription: GestureDescription?) { onCompleted?.let { mainHandler.post(it) } }
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
            override fun onCompleted(gestureDescription: GestureDescription?) { onCompleted?.let { mainHandler.post(it) } }
            override fun onCancelled(gestureDescription: GestureDescription?) { onCompleted?.let { mainHandler.post(it) } }
        }, null)
    }

    fun findNodesByText(text: String): List<AccessibilityNodeInfo> { val root = rootInActiveWindow ?: return emptyList(); return root.findAccessibilityNodeInfosByText(text)?.toList().orEmpty() }

    fun nodeBounds(node: AccessibilityNodeInfo): Rect { val r = Rect(); node.getBoundsInScreen(r); return r }

    fun enterText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedEditable(root) ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findFocusedEditable(child)
            if (res != null) return res
        }
        return null
    }

    fun captureScreenshotBlocking(timeoutMs: Long = 1500L): Bitmap? {
        if (Build.VERSION.SDK_INT < 33) return null
        val latch = CountDownLatch(1)
        var out: Bitmap? = null
        try {
            takeScreenshot(mainExecutor) { result ->
                try {
                    if (result != null) {
                        val buffer: HardwareBuffer = result.hardwareBuffer
                        val colorSpace: ColorSpace = result.colorSpace
                        val bmp = Bitmap.wrapHardwareBuffer(buffer, colorSpace)
                        out = bmp?.copy(Bitmap.Config.ARGB_8888, false)
                        bmp?.close()
                        buffer.close()
                    }
                } catch (t: Throwable) {
                    Log.w("ClickAI", "Screenshot conversion failed: ${t.message}")
                } finally {
                    latch.countDown()
                }
            }
        } catch (t: Throwable) {
            Log.w("ClickAI", "takeScreenshot failed: ${t.message}")
            latch.countDown()
        }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return out
    }
}