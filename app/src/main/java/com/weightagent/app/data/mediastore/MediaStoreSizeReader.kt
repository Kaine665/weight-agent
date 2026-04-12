package com.weightagent.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreSizeReader {

    suspend fun readSizeBytes(context: Context, mediaStoreId: Long): Long? = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val uri = ContentUris.withAppendedId(collection, mediaStoreId)
        val projection = arrayOf(MediaStore.Audio.Media.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@withContext null
            val idx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            c.getLong(idx)
        }
    }
}
