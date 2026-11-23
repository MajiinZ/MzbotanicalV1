package org.mz.killrs.auth.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mz.killrs.auth.AuthViewModel

@Composable
internal fun PasswordResetDialog(
    initialEmail: String,
    auth: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
) {
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Reset your password") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (sent) {
                    Text("If an account exists for $email, check your inbox or spam folder. Press and hold the reset link to copy it without opening it, then paste it here. If the email has a code, enter that instead.")
                    OutlinedTextField(code, { code = it.take(4096) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text("Reset link or code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                    OutlinedTextField(password, { password = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    OutlinedTextField(confirmation, { confirmation = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    TextButton(onClick = {
                        sent = false
                        code = ""
                        password = ""
                        confirmation = ""
                        error = null
                    }, enabled = !busy) { Text("Change email or resend") }
                } else {
                    Text("Enter your account email to request a password reset.")
                    OutlinedTextField(email, { email = it.take(254) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        if (sent) {
                            error = auth.completePasswordReset(email, code, password, confirmation)
                            if (error == null) onSuccess(email)
                        } else {
                            email = email.trim()
                            error = auth.sendResetCode(email)
                            if (error == null) sent = true
                        }
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Please wait…" else if (sent) "Change password" else "Send reset email") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
