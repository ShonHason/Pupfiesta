package org.example.project

import android.util.Log

actual fun platformLogger(tag: String, message: String) {
    Log.i(tag, message)
}