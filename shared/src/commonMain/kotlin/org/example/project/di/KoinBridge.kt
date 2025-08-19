package org.example.project.di
import kotlin.jvm.JvmStatic

/**
 * Stable entrypoint Swift can always call.
 * Avoids relying on the generated <FileName>Kt class.
 */
object KoinBridge {
    @JvmStatic
    fun start() = initKoin()
}
