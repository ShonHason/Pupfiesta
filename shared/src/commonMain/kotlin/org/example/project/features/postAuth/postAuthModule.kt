package org.example.project.features.postAuth

import PostAuthViewModel
import org.koin.dsl.module

val postAuthModule = module {
    factory {
        PostAuthViewModel(
            gardensRepo = get(),
            firebaseRepo = get(),
            defaultLanguage = "he",
            userViewModel = get(),
        )
    }
}