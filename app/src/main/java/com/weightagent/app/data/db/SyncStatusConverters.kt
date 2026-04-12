package com.weightagent.app.data.db

import androidx.room.TypeConverter

class SyncStatusConverters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
