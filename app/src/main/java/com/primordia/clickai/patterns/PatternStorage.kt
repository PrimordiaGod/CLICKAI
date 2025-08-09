package com.primordia.clickai.patterns

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object PatternStorage {
    private val gson = Gson()
    private const val FILE_NAME = "patterns.json"

    fun save(context: Context, pattern: Pattern) {
        val all = loadAll(context).toMutableList()
        val idx = all.indexOfFirst { it.name == pattern.name }
        if (idx >= 0) all[idx] = pattern else all.add(pattern)
        writeAll(context, all)
    }

    fun loadAll(context: Context): List<Pattern> {
        val f = File(context.filesDir, FILE_NAME)
        if (!f.exists()) return emptyList()
        val json = f.readText()
        val type = object : TypeToken<List<Pattern>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun loadByName(context: Context, name: String): Pattern? = loadAll(context).firstOrNull { it.name == name }

    private fun writeAll(context: Context, all: List<Pattern>) {
        val f = File(context.filesDir, FILE_NAME)
        f.writeText(gson.toJson(all))
    }
}