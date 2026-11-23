package org.mz.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.ktor.http.Url
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mz.data.supabase.MzSupabase
import org.mz.data.supabase.SupabaseConfig
import org.mz.data.supabase.readableMessage

/** Sign-in with Supabase Auth: email + password, or Google. */
class AuthRepository {
    private val auth get() = MzSupabase.client.auth

    fun currentUserId(): String? = MzSupabase.currentUserId()

    suspend fun restoreUserId(): String? {
        auth.awaitInitialization()
        return currentUserId()
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> = recovery {
        resetPasswordForEmail(email.trim(), redirectUrl = null)
    }

    suspend fun resetPassword(email: String, proof: String, newPassword: String): Result<Unit> = recovery {
        if (proof.matches(Regex("[0-9]{6,10}"))) {
            verifyEmailOtp(OtpType.Email.RECOVERY, email.trim(), proof)
        } else {
            val tokenHash = recoveryLinkToken(proof) ?: error("Invalid recovery link")
            // Verify only with our Supabase project. Do not open the link or follow its redirect.
            verifyEmailOtp(OtpType.Email.RECOVERY, tokenHash)
            check(currentUserOrNull()?.email.equals(email.trim(), ignoreCase = true)) { "Wrong recovery account" }
        }
        try {
            updateUser { password = newPassword }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Password change not confirmed. Try signing in with the new password, or request a new code.", e)
        }
    }

    // Recovery creates an authenticated session. Isolate it from the app's saved session
    // so a reset code can never put the app into a persistent signed-in state.
    private suspend fun recovery(block: suspend Auth.() -> Unit): Result<Unit> {
        val client = createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.PUBLISHABLE_KEY) {
            install(Auth) { minimalSettings() }
        }
        return try {
            client.auth.block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            withContext(NonCancellable) {
                withTimeoutOrNull(2_000) {
                    try { client.auth.signOut() } catch (e: CancellationException) { throw e } catch (_: Exception) { }
                }
                client.close()
            }
        }
    }

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
