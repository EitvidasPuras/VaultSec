package com.vaultsec.vaultsec.util.cipher

import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.AlgorithmParameters
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CipherManager @Inject constructor(
    private val keyManager: KeyManager
) {

    companion object {
        private const val TAG = "com.vaultsec.vaultsec.util.cipher.CipherManager"

        private const val CIPHER_TRANSFORMATION = "AES_256/CBC/PKCS5Padding"
    }

    fun encrypt(input: String?): String? {
        return try {
            if (input.isNullOrEmpty()) {
                return null
            }
            val wrappedKeyAndSalt = keyManager.getKey(null)!!
            val cipher: Cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrappedKeyAndSalt.key)

            val param: AlgorithmParameters = cipher.parameters
            val ivBytes: ByteArray = param.getParameterSpec(IvParameterSpec::class.java).iv
            val encryptedByteArray = cipher.doFinal(input.toByteArray(Charsets.UTF_8))

            val encodedPackage = wrappedKeyAndSalt.saltBytes + ivBytes + encryptedByteArray
            return Base64.encodeToString(encodedPackage, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(
                "$TAG.encrypt",
                e.localizedMessage!!
            )
            null
        }
    }

    fun decrypt(input: String?): String? {
        return try {
            if (input.isNullOrEmpty()) {
                return null
            }
            val buffer = ByteBuffer.wrap(Base64.decode(input, Base64.DEFAULT))

            val cipher: Cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

            val saltBytes = ByteArray(KeyManager.SALT_LENGTH)
            buffer.get(saltBytes, 0, saltBytes.size)
            val ivBytes = ByteArray(cipher.blockSize)
            buffer.get(ivBytes, 0, ivBytes.size)
            val encryptedTextBytes = ByteArray(buffer.capacity() - saltBytes.size - ivBytes.size)
            buffer.get(encryptedTextBytes)

            val wrappedKeyAndSalt = keyManager.getKey(saltBytes)!!

            cipher.init(Cipher.DECRYPT_MODE, wrappedKeyAndSalt.key, IvParameterSpec(ivBytes))

            val decryptedTextBytes = cipher.doFinal(encryptedTextBytes)
            return String(decryptedTextBytes)
        } catch (e: Exception) {
            Log.e(
                "$TAG.decrypt",
                e.localizedMessage!!
            )
            null
        }
    }
}