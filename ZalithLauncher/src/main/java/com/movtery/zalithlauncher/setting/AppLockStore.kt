/*
 * NEXCRAFT security helpers.
 * Copyright (C) 2026 NEXCRAFT contributors.
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.setting

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the optional launcher PIN encrypted with an Android Keystore AES key.
 * The PIN is never stored as readable text.
 */
object AppLockStore {
    private const val PREFS = "nexcraft_app_lock"
    private const val PIN_DATA = "pin_data"
    private const val KEY_ALIAS = "nexcraft_app_lock_key"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(PIN_DATA)

    fun setPin(context: Context, pin: String) {
        require(pin.length in 4..8 && pin.all(Char::isDigit)) { "PIN must contain 4 to 8 digits" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PIN_DATA, Base64.encodeToString(packed, Base64.NO_WRAP))
            .apply()
    }

    fun verify(context: Context, pin: String): Boolean {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PIN_DATA, null) ?: return false
        return runCatching {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > 12)
            val iv = packed.copyOfRange(0, 12)
            val encrypted = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8) == pin
        }.getOrDefault(false)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(PIN_DATA).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
