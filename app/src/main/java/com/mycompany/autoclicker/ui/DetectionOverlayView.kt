package com.mycompany.autoclicker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintRect = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintCircle = Paint().apply {
        style = Paint.Style.FILL
    }

    var boundingRect: Rect? = null
        set(value) {
            field = value
            invalidate()
        }

    var sampleColor: Int = Color.RED
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundingRect?.let { rect ->
            canvas.drawRect(rect, paintRect)
            paintCircle.color = sampleColor
            val cx = rect.centerX().toFloat()
            val cy = rect.centerY().toFloat()
            canvas.drawCircle(cx, cy, 8f, paintCircle)
        }

        // draw user selections
        for (sel in selections) {
            canvas.drawRect(sel, userPaint)
        }
    }

    // ----- user selection support -----
    private val selections = mutableListOf<Rect>()
    private val userPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun addSelection(rect: Rect) {
        selections.add(rect)
        invalidate()
    }

    fun clearSelections() {
        selections.clear()
        invalidate()
    }

    // pattern edit callbacks
    interface BoxListener { fun onBoxDrawn(rect: Rect) }
    interface ArrowListener { fun onArrowDrawn(from: Rect, to: Rect) }

    private var boxListener: BoxListener? = null
    private var arrowListener: ArrowListener? = null

    fun setOnBoxDrawnListener(l: (Rect) -> Unit) { this.boxListener = object: BoxListener{override fun onBoxDrawn(rect: Rect)=l(rect)} }
    fun setOnArrowDrawnListener(l: (Rect, Rect) -> Unit) { this.arrowListener = object: ArrowListener{override fun onArrowDrawn(from: Rect, to: Rect)=l(from,to)} }

    // touch handling for pattern edit
    private enum class Mode { NONE, DRAW_BOX, DRAG_ARROW }
    private var mode = Mode.NONE
    private var startX = 0f
    private var startY = 0f
    private var arrowStartRect: Rect? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // check if inside an existing selection
                val within = selections.firstOrNull { it.contains(event.x.toInt(), event.y.toInt()) }
                if (within != null) {
                    mode = Mode.DRAG_ARROW
                    arrowStartRect = within
                } else {
                    mode = Mode.DRAW_BOX
                    startX = event.x; startY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                when(mode) {
                    Mode.DRAW_BOX -> {
                        val rect = Rect(startX.toInt(), startY.toInt(), event.x.toInt(), event.y.toInt())
                        addSelection(rect)
                        boxListener?.onBoxDrawn(rect)
                    }
                    Mode.DRAG_ARROW -> {
                        val endRect = selections.firstOrNull { it.contains(event.x.toInt(), event.y.toInt()) }
                        val fromR = arrowStartRect
                        if (fromR != null && endRect != null && fromR != endRect) {
                            arrowListener?.onArrowDrawn(fromR, endRect)
                        }
                    }
                    else -> {}
                }
                mode = Mode.NONE
            }
        }
        return true
    }
}