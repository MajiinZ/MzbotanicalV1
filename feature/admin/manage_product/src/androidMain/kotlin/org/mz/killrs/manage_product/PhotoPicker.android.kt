package org.mz.killrs.manage_product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.mz.killrs.shared.domain.UploadFile
import org.mz.killrs.shared.domain.uriUploadFile

actual class PhotoPicker {
    // Keep your launcher var name
    private var launcherState = mutableStateOf(false)

    actual fun open() {
        launcherState.value = true
    }

    @Composable
    actual fun InitializePhotoPicker(onImageSelect: (UploadFile?) -> Unit) {
        val openPicker by remember { launcherState }

        val context = LocalContext.current
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                onImageSelect(uriUploadFile(context, uri))
            } else {
                onImageSelect(null)
            }
            launcherState.value = false
        }

        LaunchedEffect(openPicker) {
            if (openPicker) {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}
