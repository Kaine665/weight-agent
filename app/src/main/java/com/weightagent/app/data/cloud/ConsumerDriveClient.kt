package com.weightagent.app.data.cloud

/**
 * **消费级网盘**抽象：账号授权（如 refresh_token）+ 上传到指定远端路径或目录。
 */
interface ConsumerDriveClient {

    suspend fun testConnection()

    suspend fun uploadFile(
        localAbsolutePath: String,
        remoteRelativePath: String,
    ): ConsumerDriveUploadResult
}

data class ConsumerDriveUploadResult(
    /** 网盘内文件 id，部分实现可能为空 */
    val remoteFileId: String?,
)
