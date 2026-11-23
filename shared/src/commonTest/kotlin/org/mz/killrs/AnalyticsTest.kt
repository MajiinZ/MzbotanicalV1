package org.mz.killrs

import org.mz.killrs.shared.analytics.Analytics
import org.mz.killrs.shared.analytics.AnalyticsTracker
import org.mz.killrs.shared.analytics.InMemoryAnalyticsStore
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTest {
    private class RecordingTracker : AnalyticsTracker {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()
        var currentUserId: String? = null
        override fun track(event: String, properties: Map<String, Any?>) {
            events += event to properties
        }
        override fun setUserId(userId: String) {
            currentUserId = userId
        }
        override fun reset() {
            currentUserId = null
        }
    }

    private lateinit var tracker: RecordingTracker

    @BeforeTest
    fun setUp() {
        tracker = RecordingTracker()
        Analytics.install(tracker, InMemoryAnalyticsStore())
    }

    @Test
    fun signUpIdentifiesAndTracks() {
        Analytics.signUp("uid-1", "google")
        assertEquals("uid-1", tracker.currentUserId)
        assertEquals(listOf(Analytics.Events.SIGN_UP), tracker.events.map { it.first })
        assertEquals("google", tracker.events.single().second["method"])
    }

    @Test
    fun activatedFiresOncePerUser() {
        Analytics.activatedIfFirst("uid-1", "add_to_cart")
        Analytics.activatedIfFirst("uid-1", "add_to_cart")
        Analytics.activatedIfFirst("uid-2", "add_to_cart")
        assertEquals(2, tracker.events.count { it.first == Analytics.Events.ACTIVATED })
    }

    @Test
    fun returnVisitOnlyOnLaterDay() {
        Analytics.appOpened(100) // first open ever: no return visit
        Analytics.appOpened(100) // same day: no return visit
        Analytics.appOpened(103) // three days later
        Analytics.appOpened(103)
        val visits = tracker.events.filter { it.first == Analytics.Events.RETURN_VISIT }
        assertEquals(1, visits.size)
        assertEquals(3L, visits.single().second["days_since_last_visit"])
    }

    @Test
    fun signOutResetsUser() {
        Analytics.identify("uid-1")
        Analytics.identify(null)
        assertEquals(null, tracker.currentUserId)
    }
}
