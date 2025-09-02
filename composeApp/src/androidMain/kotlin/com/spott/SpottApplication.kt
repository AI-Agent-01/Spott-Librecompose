package com.spott

import android.app.Application
import com.spott.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class SpottApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@SpottApplication)
        }
    }
}