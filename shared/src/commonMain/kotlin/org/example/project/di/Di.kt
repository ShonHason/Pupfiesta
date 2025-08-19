
package org.example.project.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

import org.example.project.data.firebase.FirebaseRepository
import org.example.project.data.firebase.RemoteFirebaseRepository
import org.example.project.data.dogGardens.GardensRepository
import org.example.project.data.dogGardens.GoogleGardensRepository
import org.example.project.data.dogGardens.DogGardensViewModel
import org.example.project.data.dogs.DogsViewModel
import org.example.project.features.postAuth.postAuthModule
import org.example.project.features.registration.UserViewModel

fun initKoin(config: KoinAppDeclaration? = null) {
    org.koin.core.context.startKoin {
        config?.invoke(this)
        modules(appModules())
    }
}
fun initKoin() = initKoin { }

fun appModules() = listOf(commonModule, platformModule, domainModule, presentationModule,
    postAuthModule
)

val domainModule = module {
//    // Domain: bind interfaces to implementations
 //   singleOf(::RemoteFirebaseRepository) bind FirebaseRepository::class
   // singleOf(::GoogleGardensRepository) bind GardensRepository::class
}
// Each platform will provide this (engine + API key)
expect val platformModule: Module

val commonModule = module {
    singleOf(::createJson)
    single { createHttpClient(get(), get()) }
    singleOf(::RemoteFirebaseRepository) bind FirebaseRepository::class
   singleOf(::GoogleGardensRepository) bind GardensRepository::class

    // Data: bind implementations to interfaces
    single<GardensRepository> {
        // We want the API key from platformModule via a named qualifier
        val apiKey: String = get(org.koin.core.qualifier.named("GOOGLE_MAPS_API_KEY"))
        GoogleGardensRepository(
            client = get(),
            apiKey = apiKey
        )
    }
}



val presentationModule = module {
    factory {
        DogGardensViewModel(
            firebaseRepo = get(),
            gardensRepo = get(),
            defaultLanguage = "he"
        )
    }
    single { DogsViewModel(firebaseRepo = get()) }
    single {
       UserViewModel(
            firebaseRepo = get()
        )
    }
}

fun createJson(): Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

fun createHttpClient(engine: HttpClientEngine, json: Json) = HttpClient(engine) {
    install(ContentNegotiation) { json(json) }
}
