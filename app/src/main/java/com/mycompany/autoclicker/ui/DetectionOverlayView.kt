package com.mycompany.autoclicker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

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
    }
}