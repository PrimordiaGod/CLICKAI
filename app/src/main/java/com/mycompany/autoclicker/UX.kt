package com.mycompany.autoclicker

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object UX {
    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    fun snack(v: View, msg: String) = Snackbar.make(v, msg, Snackbar.LENGTH_LONG).show()
}