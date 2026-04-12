package com.weightagent.app.data.upload

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object UriToFile {

    suspend fun copyToCache(
        context: Context,
        uri: Uri,
        targetName: String,
    ): File = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, targetName)
        when (uri.scheme?.lowercase()) {
            "file" -> {
                val path = uri.path ?: throw IllegalStateException("无效 file 路径")
                FileInputStream(File(path)).use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
            else -> {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(out).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("无法打开录音文件")
            }
        }
        out
    }
}
