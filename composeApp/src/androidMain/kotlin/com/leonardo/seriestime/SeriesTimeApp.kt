package com.leonardo.seriestime

import android.app.Application
import com.leonardo.seriestime.di.initKoin
import org.koin.android.ext.koin.androidContext

class SeriesTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SeriesTimeApp)
        }
    }
}
