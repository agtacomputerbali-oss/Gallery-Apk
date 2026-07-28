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
    private val sharedPreferences: SharedPreferences by lazy {
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
            context.getSharedPreferences("vault_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_VAULT_PIN)
    }

    fun savePin(pin: String): Boolean {
        return sharedPreferences.edit().putString(KEY_VAULT_PIN, pin).commit()
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = sharedPreferences.getString(KEY_VAULT_PIN, null)
        return savedPin == pin
    }

    companion object {
        private const val KEY_VAULT_PIN = "key_vault_pin"
    }
}
