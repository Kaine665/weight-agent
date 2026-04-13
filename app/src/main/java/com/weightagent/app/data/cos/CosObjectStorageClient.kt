package com.weightagent.app.data.cos

import android.content.Context
import com.weightagent.app.data.cloud.ObjectStorageClient
import com.weightagent.app.data.cloud.ObjectStorageUploadResult
import com.weightagent.app.data.settings.CosSettingsStore

/**
 * 腾讯云 COS 的 [ObjectStorageClient] 实现。
 */
class CosObjectStorageClient(
    private val context: Context,
    private val cosSettingsStore: CosSettingsStore,
    private val cosRepository: CosRepository,
) : ObjectStorageClient {

    private fun requireSettings() =
        cosSettingsStore.read()?.takeIf { it.isComplete() }
            ?: throw IllegalStateException("请先完成 COS 配置并保存")

    override suspend fun testConnection() {
        val s = requireSettings()
        cosRepository.headBucket(s)
    }

    override suspend fun uploadFile(localAbsolutePath: String, remoteObjectKey: String): ObjectStorageUploadResult {
        val s = requireSettings()
        val o = cosRepository.uploadFile(s, localAbsolutePath, remoteObjectKey)
        return ObjectStorageUploadResult(etag = o.etag)
    }
}
