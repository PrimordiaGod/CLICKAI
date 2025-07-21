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
import com.yalantis.ucrop.UCrop
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import android.app.Activity
import com.clickai.macroapp.macro.engine.MacroRecorder
import android.widget.Toast

object CorrectionDialog {
    fun show(
        context: Activity,
        screenshot: Bitmap?,
        ocrResult: String?,
        onRecord: (croppedBitmap: Bitmap?) -> Unit,
        onSave: (name: String, desc: String, croppedBitmap: Bitmap?, actions: List<MacroAction>) -> Unit,
        onCancel: () -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_correction, null)
        val imageView = view.findViewById<ImageView>(R.id.correctionScreenshot)
        val ocrText = view.findViewById<TextView>(R.id.correctionOcrText)
        val nameEdit = view.findViewById<EditText>(R.id.correctionName)
        val descEdit = view.findViewById<EditText>(R.id.correctionDesc)
        var croppedBitmap: Bitmap? = screenshot
        var correctionRecorder: MacroRecorder? = null
        if (screenshot != null) imageView.setImageBitmap(screenshot)
        ocrText.text = ocrResult ?: ""
        imageView.setOnClickListener {
            // Start cropping (unchanged)
            val tempFile = File(context.cacheDir, "correction_crop.png")
            val uri = Uri.fromFile(tempFile)
            val outUri = Uri.fromFile(File(context.cacheDir, "correction_crop_out.png"))
            MediaStore.Images.Media.insertImage(context.contentResolver, screenshot, "correction_crop", null)
            UCrop.of(uri, outUri).start(context)
        }
        AlertDialog.Builder(context)
            .setTitle("Record Correction")
            .setView(view)
            .setPositiveButton("Record") { _, _ ->
                correctionRecorder = MacroRecorder()
                Toast.makeText(context, "Correction recording started. Perform the correction gesture(s).", Toast.LENGTH_LONG).show()
                onRecord(croppedBitmap)
            }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setNeutralButton("Save") { _, _ ->
                val actions = correctionRecorder?.getActions() ?: emptyList()
                onSave(nameEdit.text.toString(), descEdit.text.toString(), croppedBitmap, actions)
            }
            .show()
    }
}