package com.spott.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(config: KoinApplication.() -> Unit = {}) = startKoin {
    config()
    modules(
        androidModule,
        commonModule
    )
}

val androidModule = module {
    // Android-specific dependencies will go here
}

// This will be replaced with the actual common module from commonMain
val commonModule = module {
    // Common dependencies will go here
}