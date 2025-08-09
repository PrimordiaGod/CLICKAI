package com.primordia.clickai.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class PatternEditorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val btnAddImage = Button(this).apply { text = "Add Image Element (stub)" }
        val btnAddText = Button(this).apply { text = "Add Text Element (stub)" }
        val btnAddColor = Button(this).apply { text = "Add Color Element (stub)" }
        val btnSave = Button(this).apply { text = "Save Pattern (stub)" }
        root.addView(btnAddImage)
        root.addView(btnAddText)
        root.addView(btnAddColor)
        root.addView(btnSave)
        setContentView(root)
        btnAddImage.setOnClickListener { Toast.makeText(this, "Image element added (stub)", Toast.LENGTH_SHORT).show() }
        btnAddText.setOnClickListener { Toast.makeText(this, "Text element added (stub)", Toast.LENGTH_SHORT).show() }
        btnAddColor.setOnClickListener { Toast.makeText(this, "Color element added (stub)", Toast.LENGTH_SHORT).show() }
        btnSave.setOnClickListener { Toast.makeText(this, "Pattern saved (stub)", Toast.LENGTH_SHORT).show() }
    }
}