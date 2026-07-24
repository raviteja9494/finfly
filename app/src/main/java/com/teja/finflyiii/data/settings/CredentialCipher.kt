/* Keystore-backed authenticated encryption for locally persisted access tokens. */
package com.teja.finflyiii.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialCipher @Inject constructor() {
    private val secretKey: SecretKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE_BITS)
                        .build()
                )
                generateKey()
            }
    }

    fun encrypt(value: String): String {
        if (value.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
            updateAAD(ASSOCIATED_DATA)
        }
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(
            FORMAT_PREFIX,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP),
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decryptOrEmpty(value: String): String {
        if (value.isBlank() || !value.startsWith("$FORMAT_PREFIX$FIELD_SEPARATOR")) return ""
        return runCatching {
            val fields = value.split(FIELD_SEPARATOR, limit = 3)
            require(fields.size == 3)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    GCMParameterSpec(GCM_TAG_SIZE_BITS, Base64.decode(fields[1], Base64.NO_WRAP)),
                )
                updateAAD(ASSOCIATED_DATA)
            }
            cipher.doFinal(Base64.decode(fields[2], Base64.NO_WRAP)).toString(Charsets.UTF_8)
        }.getOrDefault("")
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "finfly_iii_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_PREFIX = "enc1"
        const val FIELD_SEPARATOR = ":"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_SIZE_BITS = 128
        val ASSOCIATED_DATA = "com.teja.finflyiii.credentials.v1".toByteArray(Charsets.UTF_8)
    }
}
