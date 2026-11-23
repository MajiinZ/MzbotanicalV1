package org.mz.killrs.shared.analytics

/**
 * Product analytics entry point shared by every feature module.
 *
 * Feature code only calls the event helpers below. Each platform installs a real
 * [AnalyticsTracker] at startup (Android uses Amplitude, see `AmplitudeTracker`).
 * Until a tracker is installed, every call is a safe no-op, so desktop builds and
 * unit tests run without analytics.
 *
 * Core funnel events:
 *  - [Events.SIGN_UP]      first time a Firebase user gets a customer record
 *  - [Events.ACTIVATED]    first successful add-to-cart for that user
 *  - [Events.RETURN_VISIT] app opened on a later calendar day than the last open
 *  - [Events.SUBSCRIBED]   a paid subscription starts (see [subscribed])
 */
object Analytics {

    object Events {
        const val SIGN_UP = "Sign Up"
        const val ACTIVATED = "Activated"
        const val RETURN_VISIT = "Return Visit"
        const val SUBSCRIBED = "Subscribed"
    }

    private const val KEY_LAST_OPEN_DAY = "analytics_last_open_epoch_day"
    private const val KEY_ACTIVATED_PREFIX = "analytics_activated_"

    private var tracker: AnalyticsTracker = NoOpAnalyticsTracker
    private var store: AnalyticsStore = InMemoryAnalyticsStore()

    fun install(tracker: AnalyticsTracker, store: AnalyticsStore) {
        this.tracker = tracker
        this.store = store
    }

    /** Attach events to a signed-in user. Pass null on sign out. */
    fun identify(userId: String?) = safely {
        if (userId == null) tracker.reset() else tracker.setUserId(userId)
    }

    /** A new customer account was created. */
    fun signUp(userId: String, method: String) = safely {
        tracker.setUserId(userId)
        tracker.track(Events.SIGN_UP, mapOf("method" to method))
    }

    /**
     * The user reached the first key action: a successful add-to-cart.
     * Fires at most once per user on this device.
     */
    fun activatedIfFirst(userId: String, trigger: String, properties: Map<String, Any?> = emptyMap()) = safely {
        val key = KEY_ACTIVATED_PREFIX + userId
        if (store.getString(key) == null) {
            store.putString(key, "1")
            tracker.track(Events.ACTIVATED, mapOf("trigger" to trigger) + properties)
        }
    }

    /**
     * Call each time the app comes to the foreground, passing the local calendar day
     * (days since epoch). Fires Return Visit once per new day after the first open.
     */
    fun appOpened(localEpochDay: Long) = safely {
        val lastDay = store.getString(KEY_LAST_OPEN_DAY)?.toLongOrNull()
        if (lastDay != null && localEpochDay > lastDay) {
            tracker.track(
                Events.RETURN_VISIT,
                mapOf("days_since_last_visit" to (localEpochDay - lastDay))
            )
        }
        if (lastDay == null || localEpochDay > lastDay) {
            store.putString(KEY_LAST_OPEN_DAY, localEpochDay.toString())
        }
    }

    /**
     * A paid subscription started. The app has no subscription flow yet, so nothing
     * calls this today. Call it once Stripe confirms the subscription (ideally from the
     * server when `customer.subscription.created` arrives, so it can't be faked).
     */
    fun subscribed(plan: String, priceUsd: Double, interval: String) = safely {
        tracker.track(
            Events.SUBSCRIBED,
            mapOf("plan" to plan, "price_usd" to priceUsd, "billing_interval" to interval)
        )
    }

    private inline fun safely(block: () -> Unit) {
        // Analytics must never break a user flow.
        try {
            block()
        } catch (_: Exception) {
        }
    }
}

interface AnalyticsTracker {
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
    fun setUserId(userId: String)
    fun reset()
}

/** Small key-value store used to remember "first time" and "last open" markers. */
interface AnalyticsStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, properties: Map<String, Any?>) = Unit
    override fun setUserId(userId: String) = Unit
    override fun reset() = Unit
}

class InMemoryAnalyticsStore : AnalyticsStore {
    private val values = mutableMapOf<String, String>()
    override fun getString(key: String): String? = values[key]
    override fun putString(key: String, value: String) {
        values[key] = value
    }
}
