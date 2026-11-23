package org.mz.killrs

import android.content.Context
import com.amplitude.android.Amplitude
import org.mz.killrs.shared.analytics.AnalyticsStore
import org.mz.killrs.shared.analytics.AnalyticsTracker

/** Android implementation of [AnalyticsTracker] backed by the Amplitude Kotlin SDK. */
class AmplitudeTracker(apiKey: String, context: Context) : AnalyticsTracker {

    // Default autocapture is SESSIONS only: session start/end, no screen or click capture.
    private val amplitude = Amplitude(apiKey, context.applicationContext) {}

    override fun track(event: String, properties: Map<String, Any?>) {
        amplitude.track(event, properties)
    }

    override fun setUserId(userId: String) {
        amplitude.setUserId(userId)
    }

    override fun reset() {
        amplitude.reset()
    }
}

/** Persists analytics markers (last open day, activated flags) in SharedPreferences. */
class SharedPrefsAnalyticsStore(context: Context) : AnalyticsStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("mz_analytics", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
