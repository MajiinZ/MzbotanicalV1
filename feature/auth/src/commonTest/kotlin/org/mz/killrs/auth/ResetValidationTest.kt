package org.mz.killrs.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ResetValidationTest {
    @Test fun emailAllowsWhitespaceAtEdgesButNotInside() {
        assertNull(validateResetEmail(" user@example.com "))
        assertNotNull(validateResetEmail("user @example.com"))
        assertNotNull(validateResetEmail("user"))
    }

    @Test fun codeMustBeNumericAndKeepsLeadingZeros() {
        assertNull(validateResetFields("001234", "password123", "password123"))
        assertNull(validateResetFields("00123456", "password123", "password123"))
        assertNotNull(validateResetFields("01234", "password123", "password123"))
        assertNotNull(validateResetFields("01A234", "password123", "password123"))
    }

    @Test fun passwordMustMatchAndFitTheByteLimit() {
        assertNotNull(validateResetFields("001234", "short", "short"))
        assertNotNull(validateResetFields("001234", "password123", "password456"))
        val tooLong = "é".repeat(37)
        assertNotNull(validateResetFields("001234", tooLong, tooLong))
        val atLimit = "é".repeat(36)
        assertNull(validateResetFields("001234", atLimit, atLimit))
    }
}
