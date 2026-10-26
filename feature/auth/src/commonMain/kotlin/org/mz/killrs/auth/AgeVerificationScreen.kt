package org.mz.killrs.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.mz.killrs.shared.*
import org.mz.killrs.shared.component.CustomTextField
import org.mz.killrs.shared.component.PrimaryButton

@Composable
internal fun AgeVerificationScreen(store: AgeVerificationStore, onVerified: () -> Unit) {
    var month by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var denied by rememberSaveable { mutableStateOf(store.deniedBirthDate()?.let { !it.isAdult(currentAgeCheckDate()) } == true) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().background(BrandCream).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MZBotanicals", color = BrandGreen, fontSize = FontSize.EXTRA_LARGE, fontFamily = Exo2FontRegular())
        Spacer(Modifier.height(28.dp))
        Text(if (denied) "Access denied" else "Verify your age", color = BrandGreen, fontSize = FontSize.LARGE)
        Spacer(Modifier.height(12.dp))
        Text(
            if (denied) "You must be 18 or older to access MZBotanicals. Sign-in is unavailable on this device until you turn 18."
            else "You must be at least 18 to continue. Enter your date of birth before signing in.",
            color = BrandGreen, textAlign = TextAlign.Center
        )
        if (!denied) {
            Spacer(Modifier.height(24.dp))
            Text("Date of birth · MM / DD / YYYY", color = BrandGreen)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(modifier = Modifier.weight(1f), value = month,
                    onValueChange = { month = it.filter(Char::isDigit).take(2); error = null }, placeholder = "MM",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                CustomTextField(modifier = Modifier.weight(1f), value = day,
                    onValueChange = { day = it.filter(Char::isDigit).take(2); error = null }, placeholder = "DD",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                CustomTextField(modifier = Modifier.weight(1.4f), value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4); error = null }, placeholder = "YYYY",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = SurfaceError) }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = "Verify age", onClick = {
                val date = BirthDate(year.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0, day.toIntOrNull() ?: 0)
                val today = currentAgeCheckDate()
                when {
                    year.length != 4 || !date.isValid() || date.isAfter(today) -> error = "Enter a valid birth date in the past."
                    date.isAdult(today) -> onVerified()
                    else -> {
                        denied = true
                        runCatching { store.deny(date) }.onFailure {
                            error = "Access denied. Could not save age verification."
                        }
                    }
                }
            })
        }
    }
}