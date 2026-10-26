package org.mz.killrs.auth

import androidx.compose.runtime.Composable

internal data class BirthDate(val year: Int, val month: Int, val day: Int) {
    fun isValid(): Boolean {
        if (year !in 1900..9999 || month !in 1..12) return false
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val days = when (month) { 2 -> if (leap) 29 else 28; 4, 6, 9, 11 -> 30; else -> 31 }
        return day in 1..days
    }
    fun isAfter(other: BirthDate): Boolean = when {
        year != other.year -> year > other.year
        month != other.month -> month > other.month
        else -> day > other.day
    }
    fun isAdult(today: BirthDate): Boolean = !isAfter(today) &&
        (today.year - year > 18 || (today.year - year == 18 &&
            (today.month > month || today.month == month && today.day >= day)))
}

internal interface AgeVerificationStore {
    fun deniedBirthDate(): BirthDate?
    fun deny(date: BirthDate)
}

@Composable
internal expect fun rememberAgeVerificationStore(): AgeVerificationStore
internal expect fun currentAgeCheckDate(): BirthDate