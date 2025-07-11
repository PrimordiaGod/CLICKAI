package com.mycompany.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mycompany.autoclicker.cv.ColorDetector
import com.mycompany.autoclicker.cv.CvTemplateMatcher
import com.mycompany.autoclicker.ui.DetectionOverlayView
import com.mycompany.autoclicker.macro.*
import com.mycompany.autoclicker.tap.TapClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var screenGrabber: ScreenGrabber? = null
    private lateinit var preview: ImageView
    private lateinit var overlay: DetectionOverlayView
    private lateinit var tvInfo: TextView
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.preview)
        overlay = findViewById(R.id.overlay)
        tvInfo = findViewById(R.id.tvInfo)

        try {
            // warm up
            CvTemplateMatcher.match(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        } catch (_: Exception) {
            Toast.makeText(this, "OpenCV init failed", Toast.LENGTH_SHORT).show()
        }

        ScreenGrabber.requestPermission(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenGrabber.REQUEST_MEDIA_PROJECTION) {
            screenGrabber = ScreenGrabber.fromResult(this, resultCode, data)?.apply {
                val templateBitmap = generateTemplateBitmap()
                var frameCount = 0

                start { bitmap: Bitmap ->
                    runOnUiThread {
                        preview.setImageBitmap(bitmap)
                    }

                    // run detection every 10th frame (~1-2 fps) to keep UI smooth
                    if (frameCount % 10 == 0) {
                        val result = CvTemplateMatcher.matchMultiScale(bitmap, templateBitmap)
                        if (result.score > 0.8f) {
                            val rect = Rect(
                                result.x,
                                result.y,
                                (result.x + templateBitmap.width * result.scale).toInt(),
                                (result.y + templateBitmap.height * result.scale).toInt()
                            )
                            val sampledColor = ColorDetector.sampleColor(bitmap, rect.centerX(), rect.centerY())
                            runOnUiThread {
                                overlay.boundingRect = rect
                                overlay.sampleColor = sampledColor
                                tvInfo.text = "Score: %.2f Scale: %.2f\nRGB: #%06X".format(result.score, result.scale, sampledColor and 0xFFFFFF)
                            }
                        // for demo, run macro once condition satisfied
                        if (result.score > 0.9f) {
                            runMacro(bitmap)
                        }
                        }
                    frameCount++
                }
            }
        }
    }

    private fun generateTemplateBitmap(): Bitmap {
        val size = 100
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = Color.RED }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bmp
    }

    private fun runMacro(frame: Bitmap) {
        val tapper = TapClient()
        val m = macro("demo") {
            waitUntil(Condition.TemplatePresent(generateTemplateBitmap(), 0.9f)) {
                click(100, 200)
                waitMs(500)
            }
            actions {
                inputText("hello")
            }
        }
        scope.launch {
            m.execute(scope, { frame }, tapper)
        }
    }
}