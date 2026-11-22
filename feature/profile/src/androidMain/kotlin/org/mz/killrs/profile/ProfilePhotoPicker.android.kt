package org.mz.killrs.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import org.mz.killrs.shared.BrandGreen
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.mz.killrs.shared.domain.UploadFile
import org.mz.killrs.shared.domain.uriUploadFile

@Composable
actual fun ProfilePhotoPicker(enabled: Boolean, hasPhoto: Boolean, onSelected: (UploadFile) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSelected(uriUploadFile(context, uri))
    }
    TextButton(colors = ButtonDefaults.textButtonColors(contentColor = BrandGreen),
        enabled = enabled,
        onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    ) { Text(if (hasPhoto) "Change photo" else "Add profile photo") }
}
