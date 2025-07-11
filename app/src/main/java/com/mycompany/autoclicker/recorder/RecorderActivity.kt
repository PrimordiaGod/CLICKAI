package com.mycompany.autoclicker.recorder

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.mycompany.autoclicker.R
import com.mycompany.autoclicker.macro.Action
import com.mycompany.autoclicker.macro.Macro
import com.mycompany.autoclicker.ui.DetectionOverlayView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RecorderActivity : AppCompatActivity() {

    private lateinit var overlay: DetectionOverlayView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnPreview: Button

    private val recordedActions = mutableListOf<Action>()
    private var lastDownTime: Long = 0L
    private var recording = false
    private var prevActionTime: Long = 0L
    private var repeatCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorder)
        overlay = findViewById(R.id.overlay)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnPreview = findViewById(R.id.btnPreview)

        btnStart.setOnClickListener {
            recording = true
            recordedActions.clear()
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        }
        btnStop.setOnClickListener {
            recording = false
            btnStop.isEnabled = false
            btnPreview.isEnabled = true
            btnStart.isEnabled = true
        }
        btnPreview.setOnClickListener {
            previewMacro()
        }

        overlay.setOnTouchListener { _, event ->
            if (!recording) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastDownTime = System.currentTimeMillis()
                    downX = event.rawX; downY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    val delayMs = if (prevActionTime==0L) 0 else now-prevActionTime
                    val durationSwipe = (now - lastDownTime).toInt()
                    val upX = event.rawX; val upY = event.rawY
                    val dist2 = (upX-downX)*(upX-downX)+(upY-downY)*(upY-downY)
                    if (dist2 < 25*25) {
                        recordedActions.add(Action.Click(upX.toInt(), upY.toInt(), delayMs))
                    } else {
                        recordedActions.add(Action.Swipe(downX.toInt(), downY.toInt(), upX.toInt(), upY.toInt(), durationSwipe, delayMs))
                    }
                    prevActionTime = now
                }
            }
            false
        }
    }

    private var downX = 0f
    private var downY = 0f

    private fun previewMacro() {
        if (recordedActions.isEmpty()) return
        val m = Macro("preview")
        m.repeatCount = repeatCount
        m.doActions(recordedActions)
        overlay.clearGhosts()
        lifecycleScope.launch {
            repeat(repeatCount) {
                for (a in recordedActions) {
                    delay(a.delayMs)
                    when(a) {
                        is Action.Click -> overlay.flashDot(a.x, a.y)
                        is Action.Swipe -> overlay.animateSwipe(a)
                        else -> {}
                    }
                    if (a is Action.Swipe) delay(a.durationMs.toLong())
                }
            }
        }
    }
}