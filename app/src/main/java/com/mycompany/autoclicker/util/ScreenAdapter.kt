package com.mycompany.autoclicker.util

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import android.content.res.Configuration

data class ScreenMeta(val width:Int, val height:Int, val orientation:Int)

object ScreenAdapter {
    private var baseW = 1080
    private var baseH = 1920
    private var orientation = Configuration.ORIENTATION_PORTRAIT

    fun init(ctx: Context) { update(ctx) }

    private fun update(ctx: Context) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = Point()
        wm.defaultDisplay.getRealSize(p)
        baseW = p.x
        baseH = p.y
        orientation = ctx.resources.configuration.orientation
    }

    fun currentMeta(ctx: Context): ScreenMeta {
        update(ctx)
        return ScreenMeta(baseW, baseH, orientation)
    }

    data class Normalised(val nx: Float, val ny: Float)

    fun toNorm(x: Float, y: Float, ctx: Context): Normalised {
        update(ctx)
        return Normalised(x / baseW, y / baseH)
    }

    fun toScreen(n: Normalised, targetMeta: ScreenMeta = ScreenMeta(baseW, baseH, orientation)): Point {
        return Point((n.nx * targetMeta.width).toInt(), (n.ny * targetMeta.height).toInt())
    }

    fun invalidateCache() { /* called on display change */ }
}