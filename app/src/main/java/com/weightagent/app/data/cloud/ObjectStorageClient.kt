package com.weightagent.app.data.cloud

/**
 * **对象存储**抽象：桶 + AK/SK + region 等，按「远程路径 / 对象键」上传。
 */
interface ObjectStorageClient {

    /** 校验凭证与桶是否可用（如 head bucket）。 */
    suspend fun testConnection()

    suspend fun uploadFile(
        localAbsolutePath: String,
        remoteObjectKey: String,
    ): ObjectStorageUploadResult
}

data class ObjectStorageUploadResult(
    val etag: String?,
)
