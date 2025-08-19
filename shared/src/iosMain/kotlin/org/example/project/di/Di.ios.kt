package org.example.project.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSBundle

// iOS Koin bindings
actual val platformModule: Module = module {
    // Ktor engine for iOS
    single<HttpClientEngine> { Darwin.create() }

    // Read your Google API key from Info.plist -> GOOGLE_PLACES_API_KEY
    single(named("GOOGLE_MAPS_API_KEY")) {
        (NSBundle.mainBundle.objectForInfoDictionaryKey("GOOGLE_PLACES_API_KEY") as? String)
            ?: ""
    }
}
