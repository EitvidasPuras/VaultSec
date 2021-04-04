package com.vaultsec.vaultsec.util.cipher

import android.util.Log
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import com.vaultsec.vaultsec.util.hashString
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManager @Inject constructor(
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences
) {

    companion object {
        private const val SALT_LENGTH = 128
        private const val ITERATION_COUNT = 30000
        private const val KEY_LENGTH = 256

        private const val SECRET_KEY_FACTORY_ALG = "PBEwithHmacSHA256AndAES_256"
        private const val SECRET_KEY_SPEC_ALG = "AES"
    }

    val edKey: SecretKeySpec? = generateKey()

    private fun getRandomBytes(): ByteArray {
        val ba = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(ba)
        return ba
    }

    private fun generateKey(): SecretKeySpec? {
        return try {
            val credentials = encryptedSharedPrefs.getCredentials() ?: return null
            val saltBytes: ByteArray = getRandomBytes()
            encryptedSharedPrefs.storeSalt(saltBytes)
            val factory: SecretKeyFactory =
                SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALG)
            val spec = PBEKeySpec(
                (hashString(credentials.emailHash, 1) + hashString(
                    credentials.passwordHash,
                    1
                )).toCharArray(),
                encryptedSharedPrefs.getSalt(), ITERATION_COUNT, KEY_LENGTH
            )
            val secretKey: SecretKey = factory.generateSecret(spec)
            val secretKeySpec = SecretKeySpec(secretKey.encoded, SECRET_KEY_SPEC_ALG)
            secretKeySpec
        } catch (e: Exception) {
            Log.e("com.vaultsec.vaultsec.util.cipher.KeyManager.generateKey", e.message!!)
            null
        }
    }
}