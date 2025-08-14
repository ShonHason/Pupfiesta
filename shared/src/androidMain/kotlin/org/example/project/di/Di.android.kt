
package org.example.project.di

import android.content.Context
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.example.project.shared.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

actual val platformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single(named("GOOGLE_MAPS_API_KEY")) { BuildConfig.GOOGLE_MAPS_API_KEY }
}
fun startKoinIfNeeded(context: Context) {
    if (GlobalContext.getOrNull() == null) {
        initKoin { androidContext(context) }
    }
}