package com.mycompany.autoclicker.tap

import android.util.Log
import com.mycompany.autoclicker.macro.TapInterface
import java.io.DataOutputStream

class TapClient: TapInterface {

    private fun exec(cmd: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.waitFor()
        } catch (e: Exception) {
            Log.e("TapClient", "exec error", e)
        }
    }

    override fun tap(x: Int, y: Int) {
        exec("input tap $x $y")
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }

    override fun inputText(text: String) {
        exec("input text '${text.replace(" ", "%s")}'")
    }
}