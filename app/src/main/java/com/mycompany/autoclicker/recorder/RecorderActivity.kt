package com.mycompany.autoclicker.recorder

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mycompany.autoclicker.R
import com.mycompany.autoclicker.macro.Action
import com.mycompany.autoclicker.macro.Macro
import com.mycompany.autoclicker.ui.DetectionOverlayView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import com.mycompany.autoclicker.util.ScreenAdapter
import androidx.core.content.FileProvider
import java.io.File
import android.app.Dialog
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mycompany.autoclicker.recorder.DelayAdapter

class RecorderActivity : AppCompatActivity() {

    private lateinit var overlay: DetectionOverlayView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnPreview: Button
    private lateinit var btnSave: Button

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
        btnSave = findViewById(R.id.btnSave)

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
            showParamDialog()
        }
        btnPreview.setOnClickListener {
            previewMacro()
        }
        btnSave.setOnClickListener {
            saveMacro()
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
                        val norm = ScreenAdapter.toNorm(upX, upY, this)
                        recordedActions.add(Action.ClickNorm(norm, delayMs))
                    } else {
                        val nStart = ScreenAdapter.toNorm(downX, downY, this)
                        val nEnd = ScreenAdapter.toNorm(upX, upY, this)
                        recordedActions.add(Action.SwipeNorm(nStart, nEnd, durationSwipe, delayMs))
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
                        is Action.ClickNorm -> {
                            val pt = ScreenAdapter.toScreen(a.n)
                            overlay.flashDot(pt.x, pt.y)
                        }
                        is Action.SwipeNorm -> {
                            val s = ScreenAdapter.toScreen(a.start)
                            val e = ScreenAdapter.toScreen(a.end)
                            overlay.animateSwipe(Action.Swipe(s.x, s.y, e.x, e.y, a.durationMs))
                        }
                        is Action.Click -> overlay.flashDot(a.x, a.y)
                        is Action.Swipe -> overlay.animateSwipe(a)
                        else -> {}
                    }
                    if (a is Action.Swipe) delay(a.durationMs.toLong())
                }
            }
        }
    }

    private fun showParamDialog() {
        val dlg = Dialog(this)
        dlg.setContentView(R.layout.dialog_action_editor)
        val etRepeat = dlg.findViewById<EditText>(R.id.etRepeat)
        etRepeat.setText(repeatCount.toString())
        val rv = dlg.findViewById<RecyclerView>(R.id.rvActions)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = DelayAdapter(recordedActions)
        dlg.findViewById<Button>(R.id.btnOK).setOnClickListener {
            repeatCount = etRepeat.text.toString().toIntOrNull() ?: 1
            dlg.dismiss()
        }
        dlg.show()
    }

    private fun saveMacro() {
        val macro = Macro("recorded").apply {
            repeatCount = this@RecorderActivity.repeatCount
            doActions(recordedActions)
        }
        val file = com.mycompany.autoclicker.macro.MacroStorage.save(this, macro, "recorded_${System.currentTimeMillis()}")
        // Share intent
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, "Share Macro"))
    }
}