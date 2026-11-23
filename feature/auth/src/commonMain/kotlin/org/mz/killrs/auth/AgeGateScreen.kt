package org.mz.killrs.auth

import androidx.compose.runtime.Composable

/**
 * Navigation 3 entry for the 18+ age gate.
 *
 * The gate used to be a boolean inside [AuthenticationScreen]. It is now its own
 * destination ([org.mz.killrs.shared.navigation.Screen.AgeGate]) at the root of the
 * back stack; on success the caller replaces the whole stack with the sign-in screen,
 * so Back can never return to (or skip) the gate.
 */
@Composable
fun AgeGateScreen(onVerified: () -> Unit) {
    val store = rememberAgeVerificationStore()
    AgeVerificationScreen(store = store, onVerified = onVerified)
}
