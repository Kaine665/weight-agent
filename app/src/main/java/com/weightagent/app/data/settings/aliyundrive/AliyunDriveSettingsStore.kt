package com.weightagent.app.data.settings.aliyundrive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aliyunDriveDataStore: DataStore<Preferences> by preferencesDataStore(name = "aliyun_drive_settings")

class AliyunDriveSettingsStore(private val context: Context) {

    private val keyRefresh = stringPreferencesKey("refresh_token")
    private val keyFolder = stringPreferencesKey("remote_folder")

    val settings: Flow<AliyunDriveSettings?> = context.aliyunDriveDataStore.data.map { prefs ->
        val rt = prefs[keyRefresh]?.trim().orEmpty()
        if (rt.isBlank()) {
            null
        } else {
            AliyunDriveSettings(
                refreshToken = rt,
                remoteFolder = prefs[keyFolder]?.trim().orEmpty().ifBlank { "recordings" },
            )
        }
    }

    suspend fun read(): AliyunDriveSettings? = settings.first()

    suspend fun save(settings: AliyunDriveSettings) {
        context.aliyunDriveDataStore.edit {
            it[keyRefresh] = settings.refreshToken.trim()
            it[keyFolder] = settings.normalizedRemoteFolder()
        }
    }

    suspend fun clear() {
        context.aliyunDriveDataStore.edit {
            it.remove(keyRefresh)
            it.remove(keyFolder)
        }
    }
}
