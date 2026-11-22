package org.mz.killrs.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mz.data.AuthRepository
import org.mz.data.domain.CustomerRepository

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    /** [idToken] comes from Google Sign-In on Android; null opens the browser flow (desktop). */
    fun signInWithGoogle(idToken: String?, onSuccess: () -> Unit, onError: (String) -> Unit) = run(onSuccess, onError) {
        if (idToken != null) authRepository.signInWithGoogleIdToken(idToken) else authRepository.signInWithGoogleBrowser()
    }

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        validate(email, password)?.let { return onError(it) }
        run(onSuccess, onError) { authRepository.signInWithEmail(email, password) }
    }

    /** [onConfirmEmail] is called when Supabase requires the user to confirm their address first. */
    fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String,
        onSuccess: () -> Unit,
        onConfirmEmail: () -> Unit,
        onError: (String) -> Unit,
    ) {
        validate(email, password)?.let { return onError(it) }
        if (password.length < 8) return onError("Use a password with at least 8 characters.")
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.signUpWithEmail(email, password, fullName)
                .onSuccess { signedIn ->
                    if (signedIn) ensureCustomer(onSuccess, onError)
                    else withContext(Dispatchers.Main) { onConfirmEmail() }
                }
                .onFailure { withContext(Dispatchers.Main) { onError(it.message ?: "Could not create the account.") } }
        }
    }

    private fun validate(email: String, password: String): String? = when {
        !email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) -> "Enter a valid email address."
        password.isEmpty() -> "Enter your password."
        else -> null
    }

    private fun run(onSuccess: () -> Unit, onError: (String) -> Unit, signIn: suspend () -> Result<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            signIn()
                .onSuccess { ensureCustomer(onSuccess, onError) }
                .onFailure { withContext(Dispatchers.Main) { onError(it.message ?: "Sign-in failed.") } }
        }
    }

    private suspend fun ensureCustomer(onSuccess: () -> Unit, onError: (String) -> Unit) {
        customerRepository.createCustomer(
            onSuccess = { viewModelScope.launch(Dispatchers.Main) { onSuccess() } },
            onError = { message -> viewModelScope.launch(Dispatchers.Main) { onError(message) } },
        )
    }
}
