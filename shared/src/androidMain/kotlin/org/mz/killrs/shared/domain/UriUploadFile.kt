package org.mz.killrs.shared.domain

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024

/** Reads a picked image (content:// URI) lazily, refusing files larger than 10 MB. */
fun uriUploadFile(context: Context, uri: Uri): UploadFile {
    val resolver = context.applicationContext.contentResolver
    return UploadFile {
        withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    check(out.size() <= MAX_UPLOAD_BYTES) { "Choose an image smaller than 10 MB." }
                }
                val bytes = out.toByteArray()
                check(bytes.isNotEmpty()) { "The selected image is empty." }
                bytes
            } ?: error("Could not open the selected image.")
        }
    }
}
