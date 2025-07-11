package com.mycompany.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var screenGrabber: ScreenGrabber? = null
    private lateinit var preview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preview = ImageView(this)
        setContentView(preview)

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