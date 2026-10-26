package org.mz.killrs.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import java.time.LocalDate

@Composable
internal actual fun rememberAgeVerificationStore(): AgeVerificationStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val prefs = context.getSharedPreferences("age_verification", Context.MODE_PRIVATE)
        object : AgeVerificationStore {
            override fun deniedBirthDate(): BirthDate? {
                val year = prefs.getInt("year", 0)
                return if (year == 0) null else BirthDate(year, prefs.getInt("month", 0), prefs.getInt("day", 0))
            }
            override fun deny(date: BirthDate) {
                check(prefs.edit().putInt("year", date.year).putInt("month", date.month)
                    .putInt("day", date.day).commit()) { "Could not save age verification. Please try again." }
            }
        }
    }
}

internal actual fun currentAgeCheckDate(): BirthDate = LocalDate.now().let { BirthDate(it.year, it.monthValue, it.dayOfMonth) }