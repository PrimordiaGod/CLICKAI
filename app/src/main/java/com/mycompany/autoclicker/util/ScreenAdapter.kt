package com.mycompany.autoclicker.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

object ScreenAdapter {
    private var baseW = 1080
    private var baseH = 1920

    fun init(ctx: Context) { update(ctx) }

    private fun update(ctx: Context) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getRealSize(p)
        baseW = p.x
        baseH = p.y
    }

    data class Normalised(val nx: Float, val ny: Float)

    fun toNorm(x: Float, y: Float, ctx: Context): Normalised {
        update(ctx)
        return Normalised(x / baseW, y / baseH)
    }

    fun toScreen(n: Normalised): Point {
        return Point((n.nx * baseW).toInt(), (n.ny * baseH).toInt())
    }
}