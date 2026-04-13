package com.weightagent.app.data.aliyundrive

import com.weightagent.app.data.cloud.ConsumerDriveClient
import com.weightagent.app.data.cloud.ConsumerDriveUploadResult
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettingsStore
import java.io.File

class AliyunDriveConsumerClient(
    private val store: AliyunDriveSettingsStore,
    private val repository: AliyunDriveRepository,
) : ConsumerDriveClient {

    private suspend fun requireSettings() =
        store.read()?.takeIf { it.isComplete() }
            ?: throw IllegalStateException("请先填写并保存阿里云盘 refresh_token")

    override suspend fun testConnection() {
        val s = requireSettings()
        repository.refreshSession(s)
    }

    override suspend fun uploadFile(localAbsolutePath: String, remoteRelativePath: String): ConsumerDriveUploadResult {
        val s = requireSettings()
        val id = repository.uploadFile(s, File(localAbsolutePath), remoteRelativePath)
        return ConsumerDriveUploadResult(remoteFileId = id)
    }
}
