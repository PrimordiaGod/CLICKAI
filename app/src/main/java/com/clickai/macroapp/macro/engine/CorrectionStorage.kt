package com.clickai.macroapp.macro.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// Correction event data model
// type: "text", "template", "image"
// signature: e.g., text string, template name, or image hash
// screenshotPath: for preview
// ocrResult: for preview
// name/description: user-provided

data class CorrectionEvent(
    val id: String, // type:signature
    val type: String,
    val signature: String,
    val screenshotPath: String? = null,
    val ocrResult: String? = null,
    val actions: List<MacroAction>,
    val name: String = "",
    val description: String = ""
)

object CorrectionStorage {
    private val gson = Gson()
    private fun file(context: Context) = File(context.filesDir, "corrections.json")

    fun saveCorrection(context: Context, event: CorrectionEvent) {
        val all = loadAll(context).toMutableList()
        all.removeAll { it.id == event.id }
        all.add(event)
        file(context).writeText(gson.toJson(all))
    }

    fun getCorrection(context: Context, id: String): CorrectionEvent? {
        return loadAll(context).find { it.id == id }
    }

    fun getAll(context: Context): List<CorrectionEvent> = loadAll(context)

    fun deleteCorrection(context: Context, id: String) {
        val all = loadAll(context).toMutableList()
        all.removeAll { it.id == id }
        file(context).writeText(gson.toJson(all))
    }

    private fun loadAll(context: Context): List<CorrectionEvent> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        val type = object : TypeToken<List<CorrectionEvent>>() {}.type
        return gson.fromJson(f.readText(), type)
    }
}