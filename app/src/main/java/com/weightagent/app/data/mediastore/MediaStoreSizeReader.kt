package com.weightagent.app.data.mediastore

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreSizeReader {

    suspend fun readSizeBytes(context: Context, contentUri: String): Long? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(contentUri)
        if (uri.scheme?.equals("file", ignoreCase = true) == true) {
            val p = uri.path ?: return@withContext null
            val len = File(p).length()
            return@withContext if (len > 0L) len else null
        }
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
