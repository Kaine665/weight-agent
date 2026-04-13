package com.weightagent.app.work

import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.settings.CosSettings
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettings

internal object UploadRemotePath {

    fun forCos(settings: CosSettings, entity: RecordingEntity): String =
        UploadRecordingWorker.buildObjectKey(settings.normalizedPrefix, entity)

    fun forAliyunDrive(settings: AliyunDriveSettings, entity: RecordingEntity): String {
        val safeName = entity.displayName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "audio" }
        val ext = if ('.' in safeName) "" else ".m4a"
        val base = "${entity.recordingUuid}_$safeName$ext"
        return "${settings.normalizedRemoteFolder()}/$base"
    }

    fun build(
        kind: CloudStorageKind,
        cos: CosSettings?,
        aliyun: AliyunDriveSettings?,
        entity: RecordingEntity,
    ): String = when (kind) {
        CloudStorageKind.OBJECT_COS -> {
            val s = cos ?: throw IllegalStateException("COS 未配置")
            forCos(s, entity)
        }
        CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> {
            val s = aliyun ?: throw IllegalStateException("阿里云盘未配置")
            forAliyunDrive(s, entity)
        }
    }
}
