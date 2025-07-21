package com.clickai.macroapp.corrections

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import com.clickai.macroapp.R
import com.clickai.macroapp.macro.engine.CorrectionStorage
import com.clickai.macroapp.macro.engine.CorrectionEvent

class CorrectionsManagerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_corrections_manager)
        val listView = findViewById<ListView>(R.id.correctionsList)
        val corrections = CorrectionStorage.getAll(this)
        val adapter = CorrectionsAdapter(this, corrections)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val event = corrections[position]
            showCorrectionDetail(event)
        }
    }

    private fun showCorrectionDetail(event: CorrectionEvent) {
        val builder = android.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_correction, null)
        val imageView = view.findViewById<ImageView>(R.id.correctionScreenshot)
        val ocrText = view.findViewById<TextView>(R.id.correctionOcrText)
        val nameEdit = view.findViewById<EditText>(R.id.correctionName)
        val descEdit = view.findViewById<EditText>(R.id.correctionDesc)
        if (event.screenshotPath != null) {
            val bmp = BitmapFactory.decodeFile(event.screenshotPath)
            imageView.setImageBitmap(bmp)
        }
        ocrText.text = event.ocrResult ?: ""
        nameEdit.setText(event.name)
        descEdit.setText(event.description)
        builder.setTitle("Correction Detail")
            .setView(view)
            .setPositiveButton("Replay") { _, _ ->
                // TODO: Replay correction actions in context
                Toast.makeText(this, "Replay not implemented in this demo.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Delete") { _, _ ->
                CorrectionStorage.deleteCorrection(this, event.id)
                Toast.makeText(this, "Correction deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNeutralButton("Close", null)
            .show()
    }
}

class CorrectionsAdapter(context: Context, private val corrections: List<CorrectionEvent>) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)
    override fun getCount() = corrections.size
    override fun getItem(position: Int) = corrections[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = inflater.inflate(R.layout.correction_list_item, parent, false)
        val event = corrections[position]
        val nameView = view.findViewById<TextView>(R.id.correctionListName)
        val descView = view.findViewById<TextView>(R.id.correctionListDesc)
        val typeView = view.findViewById<TextView>(R.id.correctionListType)
        nameView.text = event.name
        descView.text = event.description
        typeView.text = event.type
        return view
    }
}