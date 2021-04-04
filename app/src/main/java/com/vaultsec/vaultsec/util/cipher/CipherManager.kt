package com.vaultsec.vaultsec.util.cipher

import android.util.Base64
import android.util.Log
import com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CipherManager @Inject constructor(
    private val keyManager: KeyManager,
    private val encryptedSharedPrefs: PasswordManagerEncryptedSharedPreferences
) {

    companion object {
        private const val CIPHER_TRANSFORMATION = "AES_256/CBC/PKCS5Padding"
    }

    fun encrypt(input: String): String? {
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keyManager.edKey)

            val param: AlgorithmParameters = cipher.parameters
            val ivBytes: ByteArray = param.getParameterSpec(IvParameterSpec::class.java).iv
            val encryptedByteArray = cipher.doFinal(input.toByteArray(Charsets.UTF_8))

            val encodedPackage = encryptedSharedPrefs.getSalt()!! + ivBytes + encryptedByteArray
            return Base64.encodeToString(encodedPackage, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(
                "com.vaultsec.vaultsec.util.cipher.CipherManager.encrypt",
                e.localizedMessage!!
            )
            null
        }
    }

    fun decrypt(input: String): String? {
        return try {
            val buffer = ByteBuffer.wrap(Base64.decode(input, Base64.DEFAULT))

            val cipher: Cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

            val saltBytes = ByteArray(encryptedSharedPrefs.getSalt()!!.size)
            buffer.get(saltBytes, 0, saltBytes.size)
            val ivBytes = ByteArray(cipher.blockSize)
            buffer.get(ivBytes, 0, ivBytes.size)
            val encryptedTextBytes = ByteArray(buffer.capacity() - saltBytes.size - ivBytes.size)
            buffer.get(encryptedTextBytes)

            cipher.init(Cipher.DECRYPT_MODE, keyManager.edKey, IvParameterSpec(ivBytes))

            val decryptedTextBytes = cipher.doFinal(encryptedTextBytes)
            return String(decryptedTextBytes)
        } catch (e: Exception) {
            Log.e(
                "com.vaultsec.vaultsec.util.cipher.CipherManager.decrypt",
                e.localizedMessage!!
            )
            null
        }
    }
}