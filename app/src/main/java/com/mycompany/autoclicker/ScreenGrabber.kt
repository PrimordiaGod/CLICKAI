package com.mycompany.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper

class ScreenGrabber(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int
) {

    private val imageReader: ImageReader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        2
    )

    fun start(onFrame: (Bitmap) -> Unit) {
        mediaProjection.createVirtualDisplay(
            "autoClickerDisplay",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            onFrame(bitmap)
        }, Handler(Looper.getMainLooper()))
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001

        fun requestPermission(activity: Activity) {
            val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            activity.startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        }

        fun fromResult(activity: Activity, resultCode: Int, data: Intent?): ScreenGrabber? {
            if (resultCode != Activity.RESULT_OK || data == null) return null
            val manager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            val metrics = activity.resources.displayMetrics
            return ScreenGrabber(activity, projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }
    }
}