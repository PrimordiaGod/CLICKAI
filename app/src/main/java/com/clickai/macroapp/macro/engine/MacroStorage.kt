package com.clickai.macroapp.macro.engine

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object MacroStorage {
    private val gson = Gson()

    fun saveMacro(context: Context, name: String, actions: List<MacroAction>) {
        val json = gson.toJson(actions)
        val file = File(context.filesDir, "$name.macro.json")
        file.writeText(json)
    }

    fun loadMacro(context: Context, name: String): List<MacroAction> {
        val file = File(context.filesDir, "$name.macro.json")
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<MacroAction>>() {}.type
        return gson.fromJson(json, type)
    }

    fun listMacros(context: Context): List<String> {
        return context.filesDir.listFiles()?.filter { it.name.endsWith(".macro.json") }?.map { it.name.removeSuffix(".macro.json") } ?: emptyList()
    }
}