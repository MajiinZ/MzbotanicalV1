package org.mz.killrs

import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize
import org.killrs.di.initializeKoin
import org.koin.android.ext.koin.androidContext
import org.mz.killrs.shared.analytics.Analytics

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin (
            config = {
                androidContext(this@MyApplication)
            }
        )
        Firebase.initialize(context = this)

        // Amplitude stays off when no key is configured (see README, "Analytics").
        val amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY
        if (amplitudeApiKey.isNotBlank()) {
            Analytics.install(
                tracker = AmplitudeTracker(amplitudeApiKey, this),
                store = SharedPrefsAnalyticsStore(this)
            )
        }
    }
}