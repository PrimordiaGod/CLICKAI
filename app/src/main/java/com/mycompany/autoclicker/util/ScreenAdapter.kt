package com.mycompany.autoclicker.util

import android.content.Context
import android.graphics.Point
import android.view.Display
import android.view.Surface
import android.view.WindowManager

object ScreenAdapter {
    private var width = 1080
    private var height = 1920
    private var rotation = Surface.ROTATION_0

    fun init(ctx: Context) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        val p = Point()
        display.getRealSize(p)
        width = p.x
        height = p.y
        rotation = display.rotation
    }

    data class Normalised(val nx: Float, val ny: Float) // 0..1

    fun toNorm(x: Int, y: Int): Normalised {
        return Normalised(x.toFloat() / width, y.toFloat() / height)
    }

    fun toScreen(n: Normalised, currentCtx: Context): Point {
        val wm = currentCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val disp = wm.defaultDisplay
        val p = Point(); disp.getRealSize(p)
        val curWidth = p.x
        val curHeight = p.y
        val sx = (n.nx * curWidth).toInt()
        val sy = (n.ny * curHeight).toInt()
        return Point(sx, sy)
    }
}