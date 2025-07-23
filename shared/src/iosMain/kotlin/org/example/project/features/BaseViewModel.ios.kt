package org.example.project.presentation.features

import kotlinx.coroutines.CoroutineScope

actual open class BaseViewModel actual constructor() {
    actual val scope: CoroutineScope
        get() = TODO("Not yet implemented")
}