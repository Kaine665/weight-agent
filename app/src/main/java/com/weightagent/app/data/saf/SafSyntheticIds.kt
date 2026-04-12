package com.weightagent.app.data.saf

import android.net.Uri

/**
 * 为 SAF 选中的目录内文件生成稳定的 Room 主键（负数区间），与 MediaStore / 本地路径合成 ID 错开。
 *
 * [MediaStoreAudioScanner] 的 Files 行约为 `-(fileId + 1e10)` 量级；[XiaomiPrivateRecorderScanner] 的原始路径约为 `-(u + 5e10)`。
 * 这里使用 **2e14** 级别的 bias，避免与常见 MediaStore `_ID` 组合后的大负数碰撞。
 */
object SafSyntheticIds {

    private const val SAF_DOCUMENT_BIAS = 200_000_000_000_000L // 2e14

    fun idForTreeDocument(treeUri: Uri, documentUri: Uri): Long {
        val key = "${treeUri}|${documentUri}"
        var h = 0L
        for (c in key) {
            h = 31 * h + c.code
        }
        val u = (h and Long.MAX_VALUE) % 9_000_000_000_000L // 9e12
        return -(u + SAF_DOCUMENT_BIAS)
    }

    fun isSafSyntheticId(id: Long): Boolean {
        val upper = -SAF_DOCUMENT_BIAS
        val lowerExclusive = -(SAF_DOCUMENT_BIAS + 9_000_000_000_000L)
        return id <= upper && id > lowerExclusive
    }
}
