package org.mz.killrs

import android.app.Application
import org.killrs.di.initializeKoin
import org.koin.android.ext.koin.androidContext

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin (
            config = {
                androidContext(this@MyApplication)
            }
        )
    }
}