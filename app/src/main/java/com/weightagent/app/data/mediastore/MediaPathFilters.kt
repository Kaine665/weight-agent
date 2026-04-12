package com.weightagent.app.data.mediastore

/**
 * 排除非「系统录音机」场景的音频（否则会与飞书会议录音等混在 MediaStore 里）。
 * **m4a 等扩展名本身不排除**；仅按路径/包名片段过滤。
 */
object MediaPathFilters {

    private val EXCLUDE_SUBSTRINGS = listOf(
        "feishu",
        "飞书",
        "lark",
        "ss.android.lark",
        "ss.android.ee",
        "com.ss.android.lark",
        "com.ss.android.ee",
        "bytedance.lark",
        "bytedance.ee",
    )

    fun shouldSkipMediaPath(relativePath: String?, dataPath: String?, displayName: String?): Boolean {
        val combined = buildString {
            relativePath?.let { append(it).append(' ') }
            dataPath?.let { append(it).append(' ') }
            displayName?.let { append(it) }
        }.lowercase()
        if (combined.isBlank()) return false
        return EXCLUDE_SUBSTRINGS.any { combined.contains(it) }
    }

    fun shouldSkipFileAbsolutePath(absolutePath: String, fileName: String): Boolean =
        shouldSkipMediaPath(relativePath = null, dataPath = absolutePath, displayName = fileName)
}
