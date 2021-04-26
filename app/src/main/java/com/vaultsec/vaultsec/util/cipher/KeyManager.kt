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
        private const val TAG = "com.vaultsec.vaultsec.util.cipher.KeyManager"

        const val SALT_LENGTH = 128
        /*
        * Iterations count should be as high as possible, as long as it doesn't impact app's performance
        * */
        private const val ITERATION_COUNT = 7000
        private const val KEY_LENGTH = 256

        private const val SECRET_KEY_FACTORY_ALG = "PBEwithHmacSHA256AndAES_256"
        private const val SECRET_KEY_SPEC_ALG = "AES"
    }

    private fun getRandomBytes(): ByteArray {
        val ba = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(ba)
        return ba
    }

    fun getKey(saltBytes: ByteArray?): Wrapper? {
        return try {
            var saltBytesLocal = byteArrayOf()
            if (saltBytes == null) {
                saltBytesLocal = getRandomBytes()
            }
            val credentials = encryptedSharedPrefs.getCredentials() ?: return null
            val factory: SecretKeyFactory =
                SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALG)
            val spec = PBEKeySpec(
                (hashString(credentials.emailHash, 1) + hashString(
                    credentials.passwordHash,
                    1
                )).toCharArray(),
                saltBytes ?: saltBytesLocal, ITERATION_COUNT, KEY_LENGTH
            )
            val secretKey: SecretKey = factory.generateSecret(spec)
            val secretKeySpec = SecretKeySpec(secretKey.encoded, SECRET_KEY_SPEC_ALG)

//            Log.e("getKey() saltBytes", saltBytes.contentToString())
//            Log.e("getKey() saltBytesLocal", saltBytesLocal.contentToString())
//            Log.e("getKey().encoded", secretKeySpec.encoded.contentToString())
//            Log.e(
//                "getKey() base64",
//                Base64.encodeToString(secretKeySpec.encoded, Base64.DEFAULT)
//            )
            Wrapper(secretKeySpec, saltBytes ?: saltBytesLocal)
        } catch (e: Exception) {
            Log.e("$TAG.getKey", e.message!!)
            null
        }
    }
}