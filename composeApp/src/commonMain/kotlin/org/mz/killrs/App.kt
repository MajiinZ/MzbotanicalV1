package org.mz.killrs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.mz.data.AuthRepository
import org.mz.data.domain.CustomerRepository
import org.mz.killrs.auth.AgeVerificationGate
import org.mz.killrs.navigation.SetupNavGraph
import org.mz.killrs.shared.navigation.Screen

@Composable
@Preview
fun AppPreview() = App()

@Composable
fun App(keepSignedIn: Boolean = false) {
    MaterialTheme {
        if (keepSignedIn) {
            AgeVerificationGate { AndroidSessionNavigation() }
        } else {
            val customerRepository = koinInject<CustomerRepository>()
            var appReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                customerRepository.signOut()
                appReady = true
            }
            if (appReady) SetupNavGraph()
        }
    }
}

@Composable
private fun AndroidSessionNavigation() {
    val auth = koinInject<AuthRepository>()
    var ready by remember { mutableStateOf(false) }
    var restoredUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(auth) {
        restoredUserId = auth.restoreUserId()
        ready = true
    }
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // A saved back stack must not override the freshly checked startup destination.
        CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
            SetupNavGraph(
                startDestination = if (restoredUserId == null) Screen.Auth else Screen.HomeGraph,
                ageAlreadyVerified = true,
                enablePasswordReset = true,
            )
        }
    }
}
