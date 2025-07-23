package org.example.project.features

import kotlinx.coroutines.CoroutineScope

actual open class BaseViewModel actual constructor() {
    actual val scope: CoroutineScope
        get() = TODO("Not yet implemented")
}