package com.clickai.macroapp.macro.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// Correction event data model
// interruptionKey: e.g., "text:Battle Started" or "template:reward"
data class CorrectionEvent(val interruptionKey: String, val actions: List<MacroAction>)

object CorrectionStorage {
    private val gson = Gson()
    private fun file(context: Context) = File(context.filesDir, "corrections.json")

    fun saveCorrection(context: Context, event: CorrectionEvent) {
        val all = loadAll(context).toMutableList()
        all.removeAll { it.interruptionKey == event.interruptionKey }
        all.add(event)
        file(context).writeText(gson.toJson(all))
    }

    fun getCorrection(context: Context, interruptionKey: String): CorrectionEvent? {
        return loadAll(context).find { it.interruptionKey == interruptionKey }
    }

    private fun loadAll(context: Context): List<CorrectionEvent> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        val type = object : TypeToken<List<CorrectionEvent>>() {}.type
        return gson.fromJson(f.readText(), type)
    }
}