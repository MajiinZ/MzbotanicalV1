package org.mz.killrs.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.util.prefs.Preferences

@Composable
internal actual fun rememberAgeVerificationStore(): AgeVerificationStore = remember {
    val prefs = Preferences.userRoot().node("org/mz/killrs/age_verification")
    object : AgeVerificationStore {
        override fun deniedBirthDate(): BirthDate? {
            val year = prefs.getInt("year", 0)
            return if (year == 0) null else BirthDate(year, prefs.getInt("month", 0), prefs.getInt("day", 0))
        }
        override fun deny(date: BirthDate) {
            prefs.putInt("year", date.year)
            prefs.putInt("month", date.month)
            prefs.putInt("day", date.day)
            prefs.flush()
        }
    }
}

internal actual fun currentAgeCheckDate(): BirthDate = LocalDate.now().let { BirthDate(it.year, it.monthValue, it.dayOfMonth) }