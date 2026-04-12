package com.weightagent.app.data.mediastore

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.weightagent.app.data.db.RecordingDao
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.data.device.OemDevice
import com.weightagent.app.data.storage.AllFilesAccessHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 扫描小米系系统录音机常写的 **应用私有目录**（`Android/data/.../files`），
 * 此类路径往往 **不进 MediaStore**，需 **Android 11+ 全部文件访问权限** 才能读取。
 */
class XiaomiPrivateRecorderScanner(
    private val context: Context,
    private val recordingDao: RecordingDao,
) {

    suspend fun scanIfApplicable(): Int = withContext(Dispatchers.IO) {
        if (!OemDevice.isXiaomiFamily()) {
            return@withContext 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !AllFilesAccessHelper.hasAllFilesAccess(context)) {
            return@withContext 0
        }
        val roots = recorderDataRoots()
        var count = 0
        for (root in roots) {
            if (!root.exists() || !root.isDirectory) continue
            count += walkAudioFiles(root, maxDepth = 8)
        }
        count
    }

    private fun recorderDataRoots(): List<File> {
        val ext = Environment.getExternalStorageDirectory() ?: return emptyList()
        val androidData = File(ext, "Android/data")
        val packages = listOf(
            "com.android.soundrecorder",
            "com.miui.soundrecorder",
        )
        val dirs = mutableListOf<File>()
        for (pkg in packages) {
            dirs.add(File(androidData, "$pkg/files"))
            dirs.add(File(androidData, pkg))
        }
        return dirs
    }

    private suspend fun walkAudioFiles(dir: File, maxDepth: Int, depth: Int = 0): Int {
        if (depth > maxDepth) return 0
        val list = dir.listFiles() ?: return 0
        var n = 0
        for (f in list) {
            if (f.isDirectory) {
                n += walkAudioFiles(f, maxDepth, depth + 1)
            } else if (f.isFile && isAudioExtension(f.name)) {
                upsertRawFile(f)
                n++
            }
        }
        return n
    }

    private fun isAudioExtension(name: String): Boolean {
        val dot = name.lastIndexOf('.')
        if (dot < 0) return false
        return when (name.substring(dot + 1).lowercase()) {
            "m4a", "aac", "mp3", "amr", "wav", "opus", "flac", "ogg", "3gp" -> true
            else -> false
        }
    }

    private suspend fun upsertRawFile(file: File) {
        val path = file.absolutePath
        val id = syntheticIdForRawPath(path)
        val existing = recordingDao.getById(id)
        val uuid = existing?.recordingUuid ?: UUID.randomUUID().toString()
        val status = existing?.syncStatus ?: SyncStatus.PENDING
        val uri = Uri.fromFile(file).toString()
        val modMs = file.lastModified()
        val size = file.length()
        val entity = RecordingEntity(
            mediaStoreId = id,
            recordingUuid = uuid,
            displayName = file.name,
            durationMs = 0L,
            dateModifiedMs = modMs,
            sizeBytes = size,
            contentUri = uri,
            syncStatus = status,
            objectKey = existing?.objectKey,
            etag = existing?.etag,
            remoteSizeBytes = existing?.remoteSizeBytes,
            lastError = existing?.lastError,
            lastStableSizeBytes = existing?.lastStableSizeBytes,
            lastStableCheckMs = existing?.lastStableCheckMs,
            contentSha256 = existing?.contentSha256,
        )
        recordingDao.upsertScanRow(entity)
    }

    companion object {
        private const val RAW_PATH_BIAS = 50_000_000_000L

        fun syntheticIdForRawPath(path: String): Long {
            var h = 0L
            for (c in path) {
                h = 31 * h + c.code
            }
            val u = (h and Long.MAX_VALUE) % 9_000_000_000L
            return -(u + RAW_PATH_BIAS)
        }

        fun isRawFileSyntheticId(id: Long): Boolean = id < -RAW_PATH_BIAS
    }
}
