package com.vaultsec.vaultsec.database

import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

data class Credentials(val passwordHash: String, val emailHash: String)

@Singleton
class PasswordManagerEncryptedSharedPreferences @Inject constructor(
    private val encryptedSharedPrefs: SharedPreferences
) {
    companion object {
        private const val TAG =
            "com.vaultsec.vaultsec.database.PasswordManagerEncryptedSharedPreferences"
    }

    fun storeCredentials(passHash: String, emailHash: String): Boolean {
        return try {
            if (!encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.PASSWORD_KEY)) {
                with(encryptedSharedPrefs.edit()) {
                    putString(EncryptedSharedPreferenceKeys.PASSWORD_KEY, passHash)
                    putString(EncryptedSharedPreferenceKeys.EMAIL_KEY, emailHash)
                    apply()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(
                "$TAG.storePassword",
                e.localizedMessage!!
            )
            false
        }
    }

    fun storeAccessToken(token: String): Boolean {
        return try {
            if (!encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.TOKEN_KEY)) {
                with(encryptedSharedPrefs.edit()) {
                    putString(
                        EncryptedSharedPreferenceKeys.TOKEN_KEY,
                        token
                    )
                    apply()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(
                "$TAG.storeAccessToken",
                e.localizedMessage!!
            )
            false
        }
    }

    fun emptyEncryptedSharedPrefs(): Boolean {
        return try {
            if (encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.PASSWORD_KEY)
                && encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.EMAIL_KEY)
                && encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.TOKEN_KEY)
            ) {
                with(encryptedSharedPrefs.edit()) {
                    remove(EncryptedSharedPreferenceKeys.PASSWORD_KEY)
                    remove(EncryptedSharedPreferenceKeys.EMAIL_KEY)
                    remove(EncryptedSharedPreferenceKeys.TOKEN_KEY)
                    apply()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(
                "$TAG.deletePassword",
                e.localizedMessage!!
            )
            false
        }
    }

    fun getCredentials(): Credentials? {
        return try {
            if (encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.PASSWORD_KEY)
                && encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.EMAIL_KEY)
            ) {
                Credentials(
                    encryptedSharedPrefs.getString(
                        EncryptedSharedPreferenceKeys.PASSWORD_KEY,
                        null
                    )!!,
                    encryptedSharedPrefs.getString(EncryptedSharedPreferenceKeys.EMAIL_KEY, null)!!
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(
                "$TAG.getPassword",
                e.localizedMessage!!
            )
            null
        }
    }

    fun getToken(): String? {
        return try {
            if (encryptedSharedPrefs.contains(EncryptedSharedPreferenceKeys.TOKEN_KEY)) {
                encryptedSharedPrefs.getString(EncryptedSharedPreferenceKeys.TOKEN_KEY, null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(
                "$TAG.getToken",
                e.localizedMessage!!
            )
            null
        }
    }

    object EncryptedSharedPreferenceKeys {
        const val PASSWORD_KEY = "UPHed"
        const val EMAIL_KEY = "UEHed"
        const val TOKEN_KEY = "AT"
    }
}