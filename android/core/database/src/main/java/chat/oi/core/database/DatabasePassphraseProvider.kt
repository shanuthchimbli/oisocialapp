package chat.oi.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object DatabasePassphraseProvider {
    private const val KEY_ALIAS = "oi_database_key"
    private const val PREFERENCES_NAME = "oi_database_key_store"
    private const val IV_KEY = "encrypted_passphrase_iv"
    private const val CIPHERTEXT_KEY = "encrypted_passphrase"

    fun getOrCreate(context: Context): ByteArray {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        val encodedIv = preferences.getString(IV_KEY, null)
        val encodedCiphertext = preferences.getString(CIPHERTEXT_KEY, null)

        if (encodedIv != null || encodedCiphertext != null) {
            check(encodedIv != null && encodedCiphertext != null) {
                "Encrypted database key is incomplete"
            }
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
            val key = getKey()
            return Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                doFinal(ciphertext)
            }
        }

        // Android Keystore keeps the wrapping key outside app-managed storage.
        val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val encryptor = Cipher.getInstance("AES/GCM/NoPadding")
        encryptor.init(Cipher.ENCRYPT_MODE, getKey())
        val ciphertext = encryptor.doFinal(passphrase)
        check(
            preferences.edit()
                .putString(IV_KEY, Base64.encodeToString(encryptor.iv, Base64.NO_WRAP))
                .putString(CIPHERTEXT_KEY, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .commit()
        ) { "Unable to persist encrypted database key" }
        return passphrase
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
