package com.weightagent.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.GeneralSecurityException

class CosSettingsStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val appContext = context.applicationContext
        val masterKeyAlias = try {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("无法创建加密主密钥", e)
        } catch (e: java.io.IOException) {
            throw RuntimeException("无法创建加密主密钥", e)
        }
        prefs = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun read(): CosSettings? {
        val sid = prefs.getString(KEY_SECRET_ID, null) ?: return null
        val sk = prefs.getString(KEY_SECRET_KEY, null) ?: return null
        val region = prefs.getString(KEY_REGION, null) ?: return null
        val bucket = prefs.getString(KEY_BUCKET, null) ?: return null
        val prefix = prefs.getString(KEY_PREFIX, "recordings/") ?: "recordings/"
        if (sid.isBlank() || sk.isBlank() || region.isBlank() || bucket.isBlank()) return null
        return CosSettings(
            secretId = sid,
            secretKey = sk,
            region = region,
            bucket = bucket,
            prefix = prefix,
        )
    }

    fun save(settings: CosSettings) {
        prefs.edit()
            .putString(KEY_SECRET_ID, settings.secretId.trim())
            .putString(KEY_SECRET_KEY, settings.secretKey.trim())
            .putString(KEY_REGION, settings.region.trim())
            .putString(KEY_BUCKET, settings.bucket.trim())
            .putString(KEY_PREFIX, settings.prefix.ifBlank { "recordings/" })
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "cos_settings_encrypted"
        private const val KEY_SECRET_ID = "secret_id"
        private const val KEY_SECRET_KEY = "secret_key"
        private const val KEY_REGION = "region"
        private const val KEY_BUCKET = "bucket"
        private const val KEY_PREFIX = "prefix"
    }
}
