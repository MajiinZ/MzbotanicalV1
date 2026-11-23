package org.mz.killrs.auth

internal fun validateResetEmail(email: String): String? =
    if (email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) null else "Enter a valid email address."

internal fun validateResetFields(code: String, password: String, confirm: String): String? = when {
    !code.matches(Regex("[0-9]{6,10}")) && !code.startsWith("https://", ignoreCase = true) ->
        "Paste the reset link or enter the numeric code from your email."
    password.length < 8 -> "Use a password with at least 8 characters."
    password.encodeToByteArray().size > 72 -> "Use a shorter password."
    password != confirm -> "The passwords do not match."
    else -> null
}
