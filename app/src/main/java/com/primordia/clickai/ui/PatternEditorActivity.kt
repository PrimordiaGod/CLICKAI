package com.primordia.clickai.ui

import android.app.Activity
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.primordia.clickai.patterns.*

class PatternEditorActivity : Activity() {
    private val boxes = mutableListOf<RectF>()
    private var startX = 0f
    private var startY = 0f
    private var drawing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val canvasView = object : View(this) {
            private val paint = Paint().apply { color = Color.RED; strokeWidth = 4f; style = Paint.Style.STROKE }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                boxes.forEach { canvas.drawRect(it, paint) }
            }
            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; drawing = true; boxes.add(RectF(startX, startY, startX, startY)); invalidate() }
                    MotionEvent.ACTION_MOVE -> { if (drawing) { boxes.last().right = event.x; boxes.last().bottom = event.y; invalidate() } }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { drawing = false; invalidate() }
                }
                return true
            }
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnClear = Button(this).apply { text = "Clear" }
        val btnSave = Button(this).apply { text = "Save (Text Stub)" }
        controls.addView(btnClear)
        controls.addView(btnSave)
        root.addView(controls, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(canvasView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        btnClear.setOnClickListener { boxes.clear(); canvasView.invalidate() }
        btnSave.setOnClickListener {
            val name = "Pattern${System.currentTimeMillis()}"
            // For now, create a text element placeholder; extend to choose types per box
            val elements = listOf<TextElement>(TextElement(id = "t1", query = "OK", isRegex = false))
            val relations = emptyList<Relation>()
            PatternStorage.save(this, Pattern(name, elements, relations))
            Toast.makeText(this, "Saved $name", Toast.LENGTH_SHORT).show()
        }
    }
}