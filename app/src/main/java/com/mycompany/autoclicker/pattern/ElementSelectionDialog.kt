package com.mycompany.autoclicker.pattern

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mycompany.autoclicker.R
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.ColorPickerDialog

class ElementSelectionDialog(
    context: Context,
    private val onElementCreated: (Element) -> Unit,
    private val regionBitmapProvider: () -> Bitmap,
    private val regionRectProvider: () -> android.graphics.Rect
) : BottomSheetDialog(context) {

    private val ACT_PICK = 900

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_element_selection, null)
        setContentView(v)

        v.findViewById<Button>(R.id.btnTemplate).setOnClickListener {
            // create template element from region bitmap
            val bmp = regionBitmapProvider()
            onElementCreated(Element.Template(bmp))
            dismiss()
        }
        v.findViewById<Button>(R.id.btnText).setOnClickListener {
            // prompt regex
            val et = EditText(context)
            et.hint = "Enter regex"
            val d = Dialog(context)
            d.setContentView(et)
            d.setTitle("Text Regex")
            d.setCancelable(true)
            d.findViewById<View>(android.R.id.content)?.post {
                et.requestFocus()
            }
            d.setOnDismissListener {
                val textRegex = et.text.toString()
                if (textRegex.isNotEmpty()) onElementCreated(Element.Text(textRegex.toRegex()))
                dismiss()
            }
            d.show()
        }
        v.findViewById<Button>(R.id.btnColor).setOnClickListener {
            ColorPickerDialog.Builder(context)
                .setTitle("Pick Color")
                .setPositiveButton("Select", object : ColorEnvelopeListener {
                    override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                        envelope?.let {
                            val rect = regionRectProvider()
                            onElementCreated(Element.Color(rect, it.color))
                        }
                        dismiss()
                    }
                })
                .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                .show()
        }
    }
}