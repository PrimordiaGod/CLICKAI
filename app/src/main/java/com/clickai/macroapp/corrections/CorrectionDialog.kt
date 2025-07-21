package com.clickai.macroapp.corrections

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.clickai.macroapp.macro.engine.MacroAction
import com.clickai.macroapp.R

object CorrectionDialog {
    fun show(
        context: Context,
        screenshot: Bitmap?,
        ocrResult: String?,
        onRecord: () -> Unit,
        onSave: (name: String, desc: String) -> Unit,
        onCancel: () -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_correction, null)
        val imageView = view.findViewById<ImageView>(R.id.correctionScreenshot)
        val ocrText = view.findViewById<TextView>(R.id.correctionOcrText)
        val nameEdit = view.findViewById<EditText>(R.id.correctionName)
        val descEdit = view.findViewById<EditText>(R.id.correctionDesc)
        if (screenshot != null) imageView.setImageBitmap(screenshot)
        ocrText.text = ocrResult ?: ""
        AlertDialog.Builder(context)
            .setTitle("Record Correction")
            .setView(view)
            .setPositiveButton("Record") { _, _ -> onRecord() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setNeutralButton("Save") { _, _ ->
                onSave(nameEdit.text.toString(), descEdit.text.toString())
            }
            .show()
    }
}