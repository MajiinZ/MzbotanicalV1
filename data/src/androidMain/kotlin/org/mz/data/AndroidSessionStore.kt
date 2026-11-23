package org.mz.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only encrypted tokens are persisted. The file is excluded from Android backup. */
class AndroidSessionStore(context: Context) : SessionManager {
    private val file = AtomicFile(File(context.noBackupFilesDir, "supabase-session-v1"))
    private val lock = Mutex()

    override suspend fun saveSession(session: UserSession) = access {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(create = true))
        }
        val payload = cipher.iv + cipher.doFinal(Json.encodeToString(session).encodeToByteArray())
        val output = file.startWrite()
        try {
            output.write(payload)
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }

    override suspend fun loadSession(): UserSession? = access {
        try {
            val payload = file.readFully()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
            }
            Json.decodeFromString<UserSession>(cipher.doFinal(payload.copyOfRange(12, payload.size)).decodeToString())
        } catch (_: Exception) {
            // Missing, corrupted or no longer decryptable: require sign-in again.
            file.delete()
            null
        }
    }

    override suspend fun deleteSession() = access {
        file.delete()
        check(!file.baseFile.exists()) { "Could not clear the saved session." }
    }

    private suspend fun <T> access(block: () -> T): T = withContext(Dispatchers.IO) {
        lock.withLock { block() }
    }

    private fun key(create: Boolean): SecretKey {
        val alias = "mzb-supabase-session-v1"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create) { "Session key missing" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }
}
