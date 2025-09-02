package com.spott.di

import android.content.Context
import com.spott.core.geocoding.GeocodingService
import com.spott.core.geocoding.MapTilerGeocodingService
import com.spott.core.analytics.Analytics
import com.spott.core.analytics.DefaultAnalytics
import com.spott.core.network.DefaultNetworkManager
import com.spott.core.network.NetworkManager
import com.spott.feature.parking.FindParkingViewModel
import com.spott.feature.parking.SearchDestinationViewModel
import com.spott.map.OfflineMapManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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
    // Network & API Services
    single<NetworkManager> { DefaultNetworkManager() }
    single<Analytics> { DefaultAnalytics() }
    single<GeocodingService> { MapTilerGeocodingService() }
    
    // Map Services
    single { OfflineMapManager(androidContext()) }
    
    // ViewModels
    viewModel { FindParkingViewModel(get()) }
    viewModel { SearchDestinationViewModel(get(), get()) }
}

// This will be replaced with the actual common module from commonMain
val commonModule = module {
    // Common dependencies will go here
}
