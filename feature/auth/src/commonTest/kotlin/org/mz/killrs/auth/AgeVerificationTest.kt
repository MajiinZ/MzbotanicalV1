package org.mz.killrs.auth

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AgeVerificationTest {
    @Test fun eighteenthBirthdayIsAllowed() {
        val today = BirthDate(2026, 10, 26)
        assertTrue(BirthDate(2008, 10, 26).isAdult(today))
        assertFalse(BirthDate(2008, 10, 27).isAdult(today))
        assertTrue(BirthDate(2008, 10, 25).isAdult(today))
        assertFalse(BirthDate(2009, 1, 1).isAdult(today))
        assertTrue(BirthDate(2007, 12, 31).isAdult(today))
        assertFalse(BirthDate(2027, 1, 1).isAdult(today))
    }
    @Test fun invalidDatesAreRejected() {
        assertFalse(BirthDate(2008, 2, 30).isValid())
        assertFalse(BirthDate(2009, 2, 29).isValid())
        assertTrue(BirthDate(2008, 2, 29).isValid())
        assertFalse(BirthDate(2008, 13, 1).isValid())
        assertFalse(BirthDate(2008, 1, 0).isValid())
        assertFalse(BirthDate(0, 1, 1).isValid())
    }
    @Test fun leapBirthdayRequiresMarchInNonLeapYear() {
        val birth = BirthDate(2008, 2, 29)
        assertFalse(birth.isAdult(BirthDate(2026, 2, 28)))
        assertTrue(birth.isAdult(BirthDate(2026, 3, 1)))
    }
}