package org.mz.data

import org.mz.data.supabase.SupabaseConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecoveryLinkTest {
    private val token = "a".repeat(56)
    private val link = "${SupabaseConfig.URL}/auth/v1/verify?token=$token&type=recovery"

    @Test fun acceptsProjectRecoveryLinkWithoutUsingItsRedirect() {
        assertEquals(token, recoveryLinkToken("$link&redirect_to=https%3A%2F%2Fexample.com"))
    }

    @Test fun rejectsOtherHostsAndSchemes() {
        val host = SupabaseConfig.URL.removePrefix("https://")
        assertNull(recoveryLinkToken(link.replace(host, "$host.example.com")))
        assertNull(recoveryLinkToken(link.replace(host, "example.com@$host")))
        assertNull(recoveryLinkToken(link.replace("https:", "http:")))
        assertNull(recoveryLinkToken(link.replace(host, "$host:444")))
    }

    @Test fun rejectsAmbiguousTokensAndOtherLinkTypes() {
        assertNull(recoveryLinkToken("$link&token=$token"))
        assertNull(recoveryLinkToken("$link&type=recovery"))
        assertNull(recoveryLinkToken(link.replace("type=recovery", "type=signup")))
        assertNull(recoveryLinkToken(link.replace("/verify", "/authorize")))
        assertNull(recoveryLinkToken("$link#access_token=fake"))
    }
}
