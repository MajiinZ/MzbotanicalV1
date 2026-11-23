package org.mz.killrs.auth.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.mz.killrs.auth.AuthViewModel

/** Email + password sign-in, with a toggle to create a new account. */
@Composable
fun EmailPasswordForm(
    loading: Boolean,
    textColor: Color,
    onSignIn: (email: String, password: String) -> Unit,
    onCreateAccount: (email: String, password: String, fullName: String) -> Unit,
    passwordReset: AuthViewModel? = null,
    onPasswordReset: () -> Unit = {},
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor, unfocusedTextColor = textColor,
        focusedLabelColor = textColor, unfocusedLabelColor = textColor.copy(alpha = 0.75f),
        focusedBorderColor = textColor, unfocusedBorderColor = textColor.copy(alpha = 0.5f),
        cursorColor = textColor,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (creating) {
            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it.take(100) },
                label = { Text("Full name") }, singleLine = true, enabled = !loading, colors = colors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it.trim().take(254) },
            label = { Text("Email") }, singleLine = true, enabled = !loading, colors = colors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it.take(72) },
            label = { Text("Password") }, singleLine = true, enabled = !loading, colors = colors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (creating) onCreateAccount(email, password, fullName) else onSignIn(email, password) },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = textColor, contentColor = Color(0xFF003D27)),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (creating) "Create account" else "Sign in") }
        if (!creating && passwordReset != null) {
            TextButton(onClick = { showReset = true }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Forgot password?", color = textColor)
            }
        }
        TextButton(onClick = { creating = !creating }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (creating) "Have an account? Sign in" else "New here? Create an account", color = textColor)
        }
    }
    if (showReset && passwordReset != null) {
        PasswordResetDialog(email, passwordReset, onDismiss = { showReset = false }, onSuccess = { resetEmail ->
            email = resetEmail
            password = ""
            showReset = false
            onPasswordReset()
        })
    }
}
