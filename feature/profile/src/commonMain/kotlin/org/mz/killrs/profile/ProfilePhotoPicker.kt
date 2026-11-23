package org.mz.killrs.profile

import androidx.compose.runtime.Composable
import org.mz.killrs.shared.domain.UploadFile

@Composable
expect fun ProfilePhotoPicker(enabled: Boolean, hasPhoto: Boolean, onSelected: (UploadFile) -> Unit)
