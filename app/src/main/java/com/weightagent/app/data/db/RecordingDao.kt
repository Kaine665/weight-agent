package com.weightagent.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY dateModifiedMs DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE mediaStoreId = :id LIMIT 1")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RecordingEntity): Long

    @Update
    suspend fun update(entity: RecordingEntity)

    @Query(
        """
        UPDATE recordings SET
            displayName = :displayName,
            durationMs = :durationMs,
            dateModifiedMs = :dateModifiedMs,
            sizeBytes = :sizeBytes,
            contentUri = :contentUri
        WHERE mediaStoreId = :mediaStoreId
        """,
    )
    suspend fun updateMetadata(
        mediaStoreId: Long,
        displayName: String,
        durationMs: Long,
        dateModifiedMs: Long,
        sizeBytes: Long,
        contentUri: String,
    )

    @Transaction
    suspend fun upsertScanRow(entity: RecordingEntity) {
        val rowId = insert(entity)
        if (rowId == -1L) {
            updateMetadata(
                mediaStoreId = entity.mediaStoreId,
                displayName = entity.displayName,
                durationMs = entity.durationMs,
                dateModifiedMs = entity.dateModifiedMs,
                sizeBytes = entity.sizeBytes,
                contentUri = entity.contentUri,
            )
        }
    }

    @Query(
        """
        SELECT * FROM recordings
        WHERE syncStatus IN ('PENDING', 'FAILED', 'PAUSED', 'UPLOADING')
        """,
    )
    suspend fun listNeedingUpload(): List<RecordingEntity>

    @Query("DELETE FROM recordings WHERE mediaStoreId = :id")
    suspend fun deleteById(id: Long)
}
