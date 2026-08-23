package com.sn00bol.dades.database

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager(private val context: Context) {
    private val masterKeyAlias = "dades_master_key"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    @Volatile
    private var cachedMasterKey: MasterKey? = null

    @Volatile
    private var cachedSecretKey: SecretKey? = null

    private fun getMasterKey(): MasterKey {
        return cachedMasterKey ?: synchronized(this) {
            cachedMasterKey ?: MasterKey.Builder(context, masterKeyAlias)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .let { builder ->
                    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                        builder.setRequestStrongBoxBacked(true)
                    }
                    builder.build().also { cachedMasterKey = it }
                }
        }
    }

    private fun getSecretKey(): SecretKey {
        return cachedSecretKey ?: synchronized(this) {
            cachedSecretKey ?: (KeyStore.getInstance(androidKeyStore).apply { load(null) }
                .getKey(masterKeyAlias, null) as SecretKey).also { cachedSecretKey = it }
        }
    }

    private fun getSharedPrefs() = EncryptedSharedPreferences.create(
        context,
        "dades_secure_prefs",
        getMasterKey(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabasePassphrase(): String {
        val sharedPrefs = getSharedPrefs()
        var passphrase = sharedPrefs.getString("db_passphrase", null)
        if (passphrase == null) {
            passphrase = UUID.randomUUID().toString() + UUID.randomUUID().toString()
            sharedPrefs.edit().putString("db_passphrase", passphrase).apply()
        }
        return passphrase ?: ""
    }

    fun isFirstRun(): Boolean {
        return getSharedPrefs().getBoolean("is_first_run", true)
    }

    fun setFirstRunCompleted() {
        getSharedPrefs().edit().putBoolean("is_first_run", false).apply()
    }

    fun encryptData(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decryptData(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(transformation)
            
            val ivSize = 12 // Standard GCM IV size
            val iv = combined.copyOfRange(0, ivSize)
            val encryptedBytes = combined.copyOfRange(ivSize, combined.size)
            
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error: Could not decrypt data"
        }
    }
}
