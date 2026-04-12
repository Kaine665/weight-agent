package com.weightagent.app.data.mediastore

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreSizeReader {

    /**
     * 按 [contentUri] 查询当前大小（支持 Audio.Media 与 Files 的 content Uri）。
     */
    suspend fun readSizeBytes(context: Context, contentUri: String): Long? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(contentUri)
        val projection = arrayOf(OpenableColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@withContext null
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx < 0) return@withContext null
            val v = c.getLong(idx)
            if (v > 0L) v else null
        }
    }
}
