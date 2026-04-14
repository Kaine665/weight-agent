package com.weightagent.app.data.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.weightagent.app.data.db.RecordingDao
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 递归扫描用户通过 SAF 授权的目录树，将音频文件写入 [RecordingEntity]（[contentUri] 为 `content://` 文档 URI）。
 */
class SafTreeAudioScanner(
    private val context: Context,
    private val recordingDao: RecordingDao,
    private val safFolderStore: SafFolderStore,
) {

    suspend fun scanPersistedTrees(): Int = withContext(Dispatchers.IO) {
        val trees = safFolderStore.readUriStrings()
        if (trees.isEmpty()) return@withContext 0
        var total = 0
        for (s in trees) {
            val treeUri = runCatching { Uri.parse(s) }.getOrNull() ?: continue
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: continue
            if (!root.exists()) continue
            total += walk(root, treeUri, depth = 0)
        }
        total
    }

    private suspend fun walk(dir: DocumentFile, treeUri: Uri, depth: Int, maxDepth: Int = 14): Int {
        if (depth > maxDepth) return 0
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            null
        } ?: return 0
        var n = 0
        for (child in children) {
            if (!child.exists()) continue
            if (child.isDirectory) {
                n += walk(child, treeUri, depth + 1, maxDepth)
            } else if (child.isFile && isAudioFileName(child.name)) {
                upsertDocument(treeUri, child)
                n++
            }
        }
        return n
    }

    private fun isAudioFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val dot = name.lastIndexOf('.')
        if (dot < 0) return false
        return when (name.substring(dot + 1).lowercase()) {
            "m4a", "aac", "mp3", "amr", "awb", "wav", "opus", "flac", "ogg", "3gp", "mp2", "caf" -> true
            else -> false
        }
    }

    private suspend fun upsertDocument(treeUri: Uri, file: DocumentFile) {
        val docUri = file.uri
        val id = SafSyntheticIds.idForTreeDocument(treeUri, docUri)
        val existing = recordingDao.getById(id)
        val uuid = existing?.recordingUuid ?: UUID.randomUUID().toString()
        val status = existing?.syncStatus ?: SyncStatus.PENDING
        val name = file.name ?: "recording"
        val size = try {
            file.length()
        } catch (_: Exception) {
            0L
        }
        val modMs = try {
            file.lastModified()
        } catch (_: Exception) {
            0L
        }
        val entity = RecordingEntity(
            mediaStoreId = id,
            recordingUuid = uuid,
            displayName = name,
            durationMs = 0L,
            dateModifiedMs = modMs,
            sizeBytes = size,
            contentUri = docUri.toString(),
            syncStatus = status,
            objectKey = existing?.objectKey,
            etag = existing?.etag,
            remoteSizeBytes = existing?.remoteSizeBytes,
            lastError = existing?.lastError,
            lastStableSizeBytes = existing?.lastStableSizeBytes,
            lastStableCheckMs = existing?.lastStableCheckMs,
            contentSha256 = existing?.contentSha256,
            syncProgressPercent = existing?.syncProgressPercent ?: 0,
        )
        recordingDao.upsertScanRow(entity)
    }
}
