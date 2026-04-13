package com.weightagent.app.data.settings.aliyundrive

/**
 * 阿里云盘 OpenAPI：使用 refresh_token 换 access_token 后上传。
 * refresh_token 见阿里云盘开放平台 / 第三方工具导出说明（请妥善保管）。
 */
data class AliyunDriveSettings(
    val refreshToken: String,
    /** 网盘内根下目录名，如 `recordings`，上传路径为 `{remoteFolder}/{uuid}_{name}` */
    val remoteFolder: String = "recordings",
) {
    fun isComplete(): Boolean = refreshToken.isNotBlank()

    fun normalizedRemoteFolder(): String {
        val s = remoteFolder.trim().trim('/').trim()
        return if (s.isBlank()) "recordings" else s
    }
}
