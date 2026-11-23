package org.mz.data

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import org.mz.data.supabase.SupabaseConfig

/** Accept only a direct, project-issued verification URL for password recovery. */
internal fun recoveryLinkToken(link: String): String? {
    if (link.length > 4096 || !link.startsWith("https://", ignoreCase = true)) return null
    val url = runCatching { Url(link) }.getOrNull() ?: return null
    if (url.protocol != URLProtocol.HTTPS || url.host != Url(SupabaseConfig.URL).host ||
        url.port != 443 || url.user != null || url.password != null ||
        url.encodedPath != "/auth/v1/verify" || url.fragment.isNotEmpty() ||
        url.parameters.getAll("type") != listOf("recovery")
    ) return null
    return url.parameters.getAll("token")?.singleOrNull()
        ?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]{32,160}")) }
}
