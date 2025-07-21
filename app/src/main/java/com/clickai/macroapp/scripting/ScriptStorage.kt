package com.clickai.macroapp.scripting

import android.content.Context
import java.io.File

object ScriptStorage {
    fun saveScript(context: Context, name: String, code: String) {
        val file = File(context.filesDir, "$name.script.kts")
        file.writeText(code)
    }

    fun loadScript(context: Context, name: String): String {
        val file = File(context.filesDir, "$name.script.kts")
        return if (file.exists()) file.readText() else ""
    }

    fun listScripts(context: Context): List<String> {
        return context.filesDir.listFiles()?.filter { it.name.endsWith(".script.kts") }?.map { it.name.removeSuffix(".script.kts") } ?: emptyList()
    }
}