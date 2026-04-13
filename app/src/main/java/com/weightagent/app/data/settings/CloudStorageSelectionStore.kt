package com.weightagent.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weightagent.app.data.cloud.CloudStorageKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cloudKindDataStore: DataStore<Preferences> by preferencesDataStore(name = "cloud_storage_kind")

/**
 * 用户选择的顶层云端类型（对象存储 vs 消费级网盘）。
 */
class CloudStorageSelectionStore(
    private val context: Context,
    private val cosSettingsStore: CosSettingsStore,
) {

    private val keyKind = stringPreferencesKey("kind")

    /** 未选择时为 null（冷启动应先进「选择云端」页）。 */
    val selectedKind: Flow<CloudStorageKind?> = context.cloudKindDataStore.data.map { prefs ->
        when (prefs[keyKind]) {
            CloudStorageKind.CONSUMER_ALIYUN_DRIVE.name -> CloudStorageKind.CONSUMER_ALIYUN_DRIVE
            CloudStorageKind.OBJECT_COS.name -> CloudStorageKind.OBJECT_COS
            else -> null
        }
    }

    suspend fun readKindOrNull(): CloudStorageKind? = selectedKind.first()

    suspend fun readKind(): CloudStorageKind =
        readKindOrNull() ?: throw IllegalStateException("尚未选择云端存储类型")

    suspend fun saveKind(kind: CloudStorageKind) {
        context.cloudKindDataStore.edit { it[keyKind] = kind.name }
    }

    /** 用户「更改上传方式」时清除选择，回到 [Routes.CLOUD_SELECT]。 */
    suspend fun clearKind() {
        context.cloudKindDataStore.edit { it.remove(keyKind) }
    }

    suspend fun hasExplicitSelection(): Boolean =
        context.cloudKindDataStore.data.first().contains(keyKind)

    /**
     * 升级自旧版：若从未写入过 kind，且已有完整 COS 配置，则默认对象存储，避免老用户被强制重选。
     */
    suspend fun migrateDefaultFromLegacyCosIfNeeded() {
        val prefs = context.cloudKindDataStore.data.first()
        if (prefs.contains(keyKind)) return
        val cos = cosSettingsStore.read()
        if (cos != null && cos.isComplete()) {
            saveKind(CloudStorageKind.OBJECT_COS)
        }
    }
}
