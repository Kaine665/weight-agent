package com.weightagent.app.data.db

import android.net.Uri

/**
 * 扫描后同一物理文件可能以多条 [RecordingEntity] 存在（不同 [mediaStoreId]）：
 * MediaStore `content://` 与文件路径 `file://`、或 Audio.Media 与 Files 索引重复。
 * 本类按 **显示名 + 大小 + 时长 + 修改时间（秒级分桶）** 聚类，只保留一条，避免列表里同时「已同步」与「未同步」。
 */
object RecordingDeduper {

    /** 修改时间分桶（毫秒），同一秒内视为同一文件 */
    private const val MOD_BUCKET_MS = 1_000L

    suspend fun dedupeAfterScan(dao: RecordingDao) {
        val all = dao.listAll()
        if (all.size < 2) return

        val byKey = LinkedHashMap<String, MutableList<RecordingEntity>>()
        for (e in all) {
            val key = fingerprint(e)
            byKey.getOrPut(key) { mutableListOf() }.add(e)
        }
        for ((_, group) in byKey) {
            if (group.size < 2) continue
            val winner = pickWinner(group)
            for (e in group) {
                if (e.mediaStoreId != winner.mediaStoreId) {
                    dao.deleteById(e.mediaStoreId)
                }
            }
        }
    }

    private fun fingerprint(e: RecordingEntity): String {
        val name = e.displayName.trim().lowercase()
        val modBucket = e.dateModifiedMs / MOD_BUCKET_MS
        return "$name|${e.sizeBytes}|${e.durationMs}|$modBucket"
    }

    /** 优先保留已同步；否则优先 content://；否则稳定取较小 mediaStoreId */
    private fun pickWinner(group: List<RecordingEntity>): RecordingEntity {
        val synced = group.filter { it.syncStatus == SyncStatus.SYNCED }
        if (synced.isNotEmpty()) {
            return synced.minWith(compareBy<RecordingEntity> { uriPriority(it.contentUri) }.thenBy { it.mediaStoreId })
        }
        return group.minWith(
            compareBy<RecordingEntity> { uriPriority(it.contentUri) }
                .thenBy { if (it.syncStatus == SyncStatus.UPLOADING) 0 else 1 }
                .thenBy { it.mediaStoreId },
        )
    }

    private fun uriPriority(uriString: String): Int =
        when (Uri.parse(uriString).scheme?.lowercase()) {
            "content" -> 0
            "file" -> 1
            else -> 2
        }
}
