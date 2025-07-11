package com.mycompany.autoclicker

import android.app.Application
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.material.color.DynamicColors

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("AppCrash", "Unhandled", e)
            android.os.Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Unexpected error: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            }
        }
    }
}