package com.weightagent.app.data.mediastore

import android.content.Context
import android.content.pm.PackageManager
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
 * 在小米系机型上，除 MediaStore 外补充扫描常见录音存储（**公共目录** + **Android/data 下录音包**，后者需全部文件访问）。
 */
class XiaomiPrivateRecorderScanner(
    private val context: Context,
    private val recordingDao: RecordingDao,
) {

    suspend fun scanIfApplicable(): Int = withContext(Dispatchers.IO) {
        if (!OemDevice.isXiaomiFamily()) {
            return@withContext 0
        }
        val ext = Environment.getExternalStorageDirectory() ?: return@withContext 0
        val androidData = File(ext, "Android/data")
        val hasManage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || AllFilesAccessHelper.hasAllFilesAccess(context)

        var count = 0
        // 1) 公共目录：不依赖全部文件访问，有读音频时部分系统仍可枚举
        for (root in publicRecorderRoots(ext)) {
            if (!root.exists() || !root.isDirectory) continue
            count += walkAudioFiles(root, maxDepth = 10)
        }
        // 2) Android/data 下录音机：需「全部文件访问」或在少数 ROM 上可被列出
        if (hasManage) {
            for (root in privateRecorderRoots(context, ext, androidData)) {
                if (!root.exists() || !root.isDirectory) continue
                count += walkAudioFiles(root, maxDepth = 10)
            }
        }
        count
    }

    private fun publicRecorderRoots(ext: File): List<File> =
        listOf(
            File(ext, "MIUI/sound_recorder"),
            File(ext, "MIUI/sound_recorder/call_rec"),
            File(ext, "Recordings/SoundRecorder"),
            File(ext, "Recordings"),
            File(ext, "Recorder"),
            File(ext, "Music/SoundRecorder"),
        )

    /** 固定包名 + 已安装匹配包名 + Android/media */
    private fun privateRecorderRoots(
        context: Context,
        ext: File,
        androidData: File,
    ): List<File> {
        val pm = context.packageManager
        val knownPkgs = mutableListOf(
            "com.android.soundrecorder",
            "com.miui.soundrecorder",
            "com.miui.voicerecord",
        )
        // 动态：已安装且包名像录音机的应用目录
        try {
            @Suppress("DEPRECATION")
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in apps) {
                val pkg = app.packageName.lowercase()
                if (pkg in knownPkgs.map { it.lowercase() }) continue
                if (pkg.contains("soundrecorder") || pkg.contains("sound_recorder") ||
                    pkg.contains("voicerecord") || (pkg.contains("miui") && pkg.contains("record"))
                ) {
                    knownPkgs.add(app.packageName)
                }
            }
        } catch (_: Exception) {
        }

        val dirs = mutableListOf<File>()
        val seen = HashSet<String>()
        fun addUnique(f: File) {
            val k = try {
                f.canonicalPath
            } catch (_: Exception) {
                f.absolutePath
            }
            if (seen.add(k)) {
                dirs.add(f)
            }
        }

        for (pkg in knownPkgs.distinct()) {
            addUnique(File(androidData, "$pkg/files"))
            addUnique(File(androidData, pkg))
            // Android 10+ 推荐媒体子目录
            addUnique(File(ext, "Android/media/$pkg/SoundRecorder"))
            addUnique(File(ext, "Android/media/$pkg/Recordings"))
            addUnique(File(ext, "Android/media/$pkg/files"))
        }

        // 兜底：遍历 Android/data 下目录名含 sound / record 的包
        runCatching {
            androidData.listFiles()?.forEach { child ->
                if (!child.isDirectory) return@forEach
                val n = child.name.lowercase()
                if (n.contains("soundrecorder") || n.contains("voicerecord") ||
                    (n.contains("record") && (n.contains("miui") || n.contains("com.android")))
                ) {
                    addUnique(File(child, "files"))
                    addUnique(child)
                }
            }
        }

        return dirs
    }

    private suspend fun walkAudioFiles(dir: File, maxDepth: Int, depth: Int = 0): Int {
        if (depth > maxDepth) return 0
        val list = try {
            dir.listFiles()
        } catch (_: SecurityException) {
            null
        } ?: return 0
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
            "m4a", "aac", "mp3", "amr", "awb", "wav", "opus", "flac", "ogg", "3gp", "mp2", "caf" -> true
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
