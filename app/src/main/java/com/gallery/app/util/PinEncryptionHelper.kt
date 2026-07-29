package com.gallery.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinEncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "encrypted_vault_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isPinSet(): Boolean {
        return try {
            sharedPreferences?.contains(KEY_VAULT_PIN) == true
        } catch (e: Exception) {
            false
        }
    }

    fun savePin(pin: String): Boolean {
        return try {
            resetFailedAttempts()
            sharedPreferences?.edit()?.putString(KEY_VAULT_PIN, pin)?.commit() ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun getRemainingLockoutTimeSeconds(): Long {
        return try {
            val lockoutTime = sharedPreferences?.getLong(KEY_LOCKOUT_TIMESTAMP, 0L) ?: 0L
            if (lockoutTime == 0L) return 0L

            val currentTime = System.currentTimeMillis()
            val diff = (lockoutTime + LOCKOUT_DURATION_MS) - currentTime
            if (diff > 0) {
                (diff + 999) / 1000
            } else {
                resetFailedAttempts()
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun verifyPin(pin: String): Boolean {
        return try {
            if (getRemainingLockoutTimeSeconds() > 0) {
                return false
            }

            val savedPin = sharedPreferences?.getString(KEY_VAULT_PIN, null)
            val isCorrect = savedPin == pin
            if (isCorrect) {
                resetFailedAttempts()
            } else {
                recordFailedAttempt()
            }
            isCorrect
        } catch (e: Exception) {
            false
        }
    }

    private fun recordFailedAttempt() {
        try {
            val attempts = (sharedPreferences?.getInt(KEY_FAILED_ATTEMPTS, 0) ?: 0) + 1
            val editor = sharedPreferences?.edit()?.putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                editor?.putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis())
            }
            editor?.apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun resetFailedAttempts() {
        try {
            sharedPreferences?.edit()
                ?.remove(KEY_FAILED_ATTEMPTS)
                ?.remove(KEY_LOCKOUT_TIMESTAMP)
                ?.apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        private const val KEY_VAULT_PIN = "key_vault_pin"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "key_lockout_timestamp"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L
    }
}
