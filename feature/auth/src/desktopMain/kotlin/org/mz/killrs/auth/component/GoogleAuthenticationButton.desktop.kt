package org.mz.killrs.auth.component

import androidx.compose.runtime.Composable

@Composable
actual fun GoogleAuthenticationButton(
    loading: Boolean,
    onClick: () -> Unit,
    onResult: (Result<String?>) -> Unit,
) {
    // Desktop has no Google Play services: signal AuthViewModel to open Supabase's browser OAuth flow.
    GoogleButton(
        loading = loading,
        onClicked = {
            onClick()
            onResult(Result.success(null))
        },
    )
}
