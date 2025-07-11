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

class RecorderActivity : AppCompatActivity() {

    private lateinit var overlay: DetectionOverlayView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnPreview: Button

    private val recordedActions = mutableListOf<Action>()
    private var lastDownTime: Long = 0L
    private var recording = false

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
            // build macro and preview (not implemented fully)
            val macro = Macro("recorded").apply {
                doActions(recordedActions)
            }
            val json = Gson().toJson(macro)
            // show JSON temporary
            android.widget.Toast.makeText(this, json.take(120), android.widget.Toast.LENGTH_LONG).show()
        }

        overlay.setOnTouchListener { _, event ->
            if (!recording) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastDownTime = System.currentTimeMillis()
                }
                MotionEvent.ACTION_UP -> {
                    val upTime = System.currentTimeMillis()
                    val duration = upTime - lastDownTime
                    val dx = event.x - event.downTime // not correct
                    val dy = 0f
                    if (duration < 200) {
                        recordedActions.add(Action.Click(event.x.toInt(), event.y.toInt()))
                    }
                }
            }
            false
        }
    }
}