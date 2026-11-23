package org.mz.killrs

import android.app.Application
import org.killrs.di.initializeKoin
import org.koin.android.ext.koin.androidContext
import org.mz.data.AndroidSessionStore
import org.mz.data.supabase.MzSupabase

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        MzSupabase.configureSessionStorage(AndroidSessionStore(this))
        initializeKoin (
            config = {
                androidContext(this@MyApplication)
            }
        )
    }
}
