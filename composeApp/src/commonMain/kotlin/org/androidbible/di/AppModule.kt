package org.androidbible.di

import com.russhwolf.settings.Settings
import org.androidbible.data.local.AndroidBibleDatabase
import org.androidbible.data.remote.ApiService
import org.androidbible.data.remote.HttpClientFactory
import org.androidbible.data.repository.*
import org.androidbible.data.sync.SyncManager
import org.androidbible.domain.repository.*
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformModule: Module

val appModule = module {
    // Database
    single {
        AndroidBibleDatabase(get())
    }

    // Settings
    single { Settings() }

    // HTTP Client
    single {
        val authRepo: AuthRepository = get()
        HttpClientFactory.create(
            baseUrl = ApiConfig.BASE_URL,
            tokenProvider = { authRepo.getToken() },
        )
    }

    // API Service
    single { ApiService(get()) }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<BibleRepository> { BibleRepositoryImpl(get(), get()) }
    single<MarkerRepository> { MarkerRepositoryImpl(get(), get()) }
    single<ProgressRepository> { ProgressRepositoryImpl(get(), get()) }
    single<ReadingPlanRepository> { ReadingPlanRepositoryImpl(get(), get()) }
    single<DevotionalRepository> { DevotionalRepositoryImpl(get()) }
    single<SongRepository> { SongRepositoryImpl(get(), get()) }
    single<UserPreferenceRepository> { UserPreferenceRepositoryImpl(get(), get()) }

    // Sync Manager
    single { SyncManager(get(), get(), get()) }
}

object ApiConfig {
    // Override this in platform-specific code or via build config
    var BASE_URL: String = "http://10.0.2.2:8000/" // Android emulator localhost
    var WS_URL: String = "ws://10.0.2.2:8080/"
}

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(appModule, platformModule)
    }
}
