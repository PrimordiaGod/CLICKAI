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

object CorrectionDialog {
    fun show(
        context: Activity,
        screenshot: Bitmap?,
        ocrResult: String?,
        onRecord: (croppedBitmap: Bitmap?) -> Unit,
        onSave: (name: String, desc: String, croppedBitmap: Bitmap?) -> Unit,
        onCancel: () -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_correction, null)
        val imageView = view.findViewById<ImageView>(R.id.correctionScreenshot)
        val ocrText = view.findViewById<TextView>(R.id.correctionOcrText)
        val nameEdit = view.findViewById<EditText>(R.id.correctionName)
        val descEdit = view.findViewById<EditText>(R.id.correctionDesc)
        var croppedBitmap: Bitmap? = screenshot
        if (screenshot != null) imageView.setImageBitmap(screenshot)
        ocrText.text = ocrResult ?: ""
        imageView.setOnClickListener {
            // Start cropping
            val tempFile = File(context.cacheDir, "correction_crop.png")
            val uri = Uri.fromFile(tempFile)
            val outUri = Uri.fromFile(File(context.cacheDir, "correction_crop_out.png"))
            MediaStore.Images.Media.insertImage(context.contentResolver, screenshot, "correction_crop", null)
            UCrop.of(uri, outUri).start(context)
        }
        AlertDialog.Builder(context)
            .setTitle("Record Correction")
            .setView(view)
            .setPositiveButton("Record") { _, _ -> onRecord(croppedBitmap) }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .setNeutralButton("Save") { _, _ ->
                onSave(nameEdit.text.toString(), descEdit.text.toString(), croppedBitmap)
            }
            .show()
    }
}