package com.mycompany.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mycompany.autoclicker.cv.CvTemplateMatcher

class MainActivity : AppCompatActivity() {

    private var screenGrabber: ScreenGrabber? = null
    private lateinit var preview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = ImageView(this)
        setContentView(preview)

        try {
            // Trigger OpenCV loading early
            CvTemplateMatcher.match(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888))
        } catch (_: Exception) {
            Toast.makeText(this, "OpenCV init failed", Toast.LENGTH_SHORT).show()
        }

        ScreenGrabber.requestPermission(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenGrabber.REQUEST_MEDIA_PROJECTION) {
            screenGrabber = ScreenGrabber.fromResult(this, resultCode, data)?.apply {
                start { bitmap: Bitmap ->
                    runOnUiThread {
                        preview.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }
}