package org.mz.killrs.auth.component

import androidx.compose.runtime.Composable

/**
 * Google sign-in button. On success it returns a Google ID token (Android) or null when the
 * platform should use Supabase's browser OAuth flow instead (desktop).
 */
@Composable
expect fun GoogleAuthenticationButton(
    loading: Boolean,
    onClick: () -> Unit,
    onResult: (Result<String?>) -> Unit,
)
