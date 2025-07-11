package com.mycompany.autoclicker.tap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.mycompany.autoclicker.macro.TapInterface
import com.mycompany.tap.ITapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TapClient(private val context: Context) : ServiceConnection, TapInterface {

    private var service: ITapService? = null
    private val fallback = ShellTapClient()

    init {
        val intent = Intent("com.mycompany.tap.TapService")
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = ITapService.Stub.asInterface(binder)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null
    }

    private suspend fun tryInvoke(block: suspend (ITapService) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                service?.let {
                    block(it)
                    return@withContext
                }
                // fallback
                block(fallback)
            } catch (e: Exception) {
                Log.e("TapClient", "inject fail", e)
            }
        }
    }

    override fun tap(x: Int, y: Int) {
        if (service != null) service?.tap(x, y) else fallback.tap(x, y)
    }

    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) {
        if (service != null) service?.swipe(x1, y1, x2, y2, durationMs) else fallback.swipe(x1, y1, x2, y2, durationMs)
    }

    override fun inputText(text: String) {
        if (service != null) service?.inputText(text) else fallback.inputText(text)
    }
}

// shell-based temporary fallback
class ShellTapClient : ITapService.Stub() {
    private fun exec(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
        } catch (_: Exception) {
        }
    }
    override fun tap(x: Int, y: Int) = exec("input tap $x $y")
    override fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int) = exec("input swipe $x1 $y1 $x2 $y2 $durationMs")
    override fun inputText(text: String?) { text?.let { exec("input text '${it.replace(" ", "%s")}'") } }
}