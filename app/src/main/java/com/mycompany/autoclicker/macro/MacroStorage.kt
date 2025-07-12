package com.mycompany.autoclicker.macro

import android.content.Context
import com.google.gson.Gson
import java.io.File

object MacroStorage {
    private const val DIR = "macros"
    private const val EXT = ".json"
    private val gson = Gson()

    private fun dir(ctx: Context): File {
        val d = File(ctx.filesDir, DIR)
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun save(ctx: Context, macro: Macro, name: String = macro.javaClass.simpleName + System.currentTimeMillis()): File {
        val file = File(dir(ctx), "$name$EXT")
        file.writeText(gson.toJson(macro))
        return file
    }

    fun load(ctx: Context, file: File): Macro? {
        return runCatching { gson.fromJson(file.readText(), Macro::class.java) }.getOrNull()
    }

    fun list(ctx: Context): List<File> {
        return dir(ctx).listFiles { f -> f.isFile && f.name.endsWith(EXT) }?.toList() ?: emptyList()
    }
}