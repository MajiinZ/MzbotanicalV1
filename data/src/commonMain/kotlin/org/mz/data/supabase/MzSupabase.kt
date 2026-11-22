package org.mz.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Public client configuration for the MZ Botanicals Supabase project.
 * The publishable key only identifies the project; row-level security in
 * supabase/migrations decides what each signed-in user can read or change.
 * Never put a service-role/secret key in the app.
 */
object SupabaseConfig {
    const val URL = "https://dwdyywvzhzjmtfzhyywm.supabase.co"
    // Legacy "anon" public key (a JWT); supabase-kt 3.1 sends it as the default bearer token.
    const val PUBLISHABLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR3ZHl5d3Z6aHpqbXRmemh5eXdtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTUzNTUxMTgsImV4cCI6MjExMDkzMTExOH0.HNePcXXbapneMM5lbcs1A2_OrqCI_my84nIhRs3cL0w"
    const val PRODUCT_IMAGES_BUCKET = "product-images"
    const val PROFILE_IMAGES_BUCKET = "profile-images"
}

/** One shared client for the whole app (Android and desktop). */
object MzSupabase {
    val client: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl = SupabaseConfig.URL, supabaseKey = SupabaseConfig.PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id
    fun currentEmail(): String? = client.auth.currentUserOrNull()?.email
}

/** Database errors raised by our SQL functions/constraints carry a readable message. */
fun Throwable.readableMessage(fallback: String): String = when (this) {
    is RestException -> error.takeIf { it.isNotBlank() } ?: fallback
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}

/** Detects the image type from its first bytes; the storage buckets accept JPEG, PNG and WebP. */
fun imageContentType(bytes: ByteArray): Pair<String, String> = when {
    bytes.size > 3 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png" to "png"
    bytes.size > 11 && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() -> "image/webp" to "webp"
    else -> "image/jpeg" to "jpg"
}
