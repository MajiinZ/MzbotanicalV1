package org.mz.killrs.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.mz.killrs.shared.domain.UploadFile

@Composable
actual fun ProfilePhotoPicker(enabled: Boolean, hasPhoto: Boolean, onSelected: (UploadFile) -> Unit) {
    Text("Change your profile photo in the Android app")
}
