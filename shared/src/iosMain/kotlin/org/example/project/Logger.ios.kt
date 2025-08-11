package org.example.project

import platform.Foundation.NSLog

actual fun platformLogger(tag: String, message: String) {
    NSLog("%s: %s", tag, message)
}