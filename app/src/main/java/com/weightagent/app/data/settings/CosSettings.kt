package com.weightagent.app.data.settings

data class CosSettings(
    val secretId: String,
    val secretKey: String,
    val region: String,
    val bucket: String,
    val prefix: String,
) {
    val normalizedPrefix: String
        get() {
            val p = prefix.trim()
            if (p.isEmpty()) return ""
            return if (p.endsWith("/")) p else "$p/"
        }

    fun isComplete(): Boolean =
        secretId.isNotBlank() &&
            secretKey.isNotBlank() &&
            region.isNotBlank() &&
            bucket.isNotBlank()
}
