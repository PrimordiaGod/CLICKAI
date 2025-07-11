package com.mycompany.autoclicker.pattern

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.mycompany.autoclicker.ui.DetectionOverlayView
import androidx.appcompat.app.AppCompatActivity

class PatternEditorActivity : AppCompatActivity() {

    private lateinit var preview: ImageView
    private lateinit var overlay: DetectionOverlayView
    private lateinit var fab: FloatingActionButton

    private val elementRects: MutableMap<Rect, Element> = mutableMapOf()
    private val rules = mutableListOf<PatternRule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pattern_editor)
        preview = findViewById(R.id.preview)
        overlay = findViewById(R.id.overlay)
        fab = findViewById(R.id.fabSave)

        // load static screenshot passed from caller
        val bmp = intent.getParcelableExtra<Bitmap>("frame")
        preview.setImageBitmap(bmp)

        overlay.setOnBoxDrawnListener { rect ->
            // Simple: every new rect is a Template placeholder
            val el = Element.Template(Bitmap.createBitmap(bmp!!, rect.left, rect.top, rect.width(), rect.height()))
            elementRects[rect] = el
        }
        overlay.setOnArrowDrawnListener { from, to ->
            val anchor = elementRects[from] ?: return@setOnArrowDrawnListener
            val rel = elementRects[to] ?: return@setOnArrowDrawnListener
            val relation = guessRelation(from, to)
            rules += PatternRule(anchor, rel, relation)
        }

        fab.setOnClickListener {
            val pattern = Pattern(rules)
            val json = Gson().toJson(pattern)
            val data = Intent().putExtra("pattern_json", json)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun guessRelation(a: Rect, b: Rect): SpatialRelation = when {
        b.top >= a.bottom -> SpatialRelation.BELOW
        b.bottom <= a.top -> SpatialRelation.ABOVE
        b.left >= a.right -> SpatialRelation.RIGHT_OF
        else -> SpatialRelation.LEFT_OF
    }
}