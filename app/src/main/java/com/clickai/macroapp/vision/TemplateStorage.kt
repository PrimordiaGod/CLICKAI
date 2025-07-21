package com.clickai.macroapp.vision

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

object TemplateStorage {
    fun saveTemplate(context: Context, name: String, bitmap: Bitmap) {
        val file = File(context.filesDir, "$name.template.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun loadTemplate(context: Context, name: String): Bitmap? {
        val file = File(context.filesDir, "$name.template.png")
        return if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun listTemplates(context: Context): List<String> {
        return context.filesDir.listFiles()?.filter { it.name.endsWith(".template.png") }?.map { it.name.removeSuffix(".template.png") } ?: emptyList()
    }

    fun deleteTemplate(context: Context, name: String) {
        val file = File(context.filesDir, "$name.template.png")
        if (file.exists()) file.delete()
    }
}