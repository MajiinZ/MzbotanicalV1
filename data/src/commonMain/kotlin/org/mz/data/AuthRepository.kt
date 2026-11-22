package org.mz.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mz.data.supabase.MzSupabase
import org.mz.data.supabase.readableMessage

/** Sign-in with Supabase Auth: email + password, or Google. */
class AuthRepository {
    private val auth get() = MzSupabase.client.auth

    fun currentUserId(): String? = MzSupabase.currentUserId()

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = attempt("Sign-in failed") {
        auth.signInWith(Email) { this.email = email.trim(); this.password = password }
    }

    /** Returns true when the account is signed in right away, false when Supabase sent a confirmation email. */
    suspend fun signUpWithEmail(email: String, password: String, fullName: String): Result<Boolean> = runCatchingAuth("Could not create the account") {
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            if (fullName.isNotBlank()) data = buildJsonObject { put("full_name", fullName.trim()) }
        }
        auth.currentSessionOrNull() != null
    }

    /** Android: exchanges a Google ID token from Google Sign-In for a Supabase session. */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> = attempt("Google sign-in failed") {
        auth.signInWith(IDToken) { this.idToken = idToken; provider = Google }
    }

    /** Desktop: opens the browser for Google OAuth and waits for the local callback. */
    suspend fun signInWithGoogleBrowser(): Result<Unit> = attempt("Google sign-in failed") {
        auth.signInWith(Google)
    }

    suspend fun signOut() {
        try { auth.signOut() } catch (e: CancellationException) { throw e } catch (_: Exception) { }
    }

    private suspend fun attempt(prefix: String, block: suspend () -> Unit): Result<Unit> = runCatchingAuth(prefix, block)

    private suspend fun <T> runCatchingAuth(prefix: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException("$prefix: ${e.readableMessage("check your connection and try again")}", e))
    }
}
