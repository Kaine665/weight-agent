package com.weightagent.app.di

import android.content.Context
import androidx.work.WorkManager
import com.weightagent.app.data.aliyundrive.AliyunDriveConsumerClient
import com.weightagent.app.data.aliyundrive.AliyunDriveRepository
import com.weightagent.app.data.cloud.CloudUploadGateway
import com.weightagent.app.data.cos.CosObjectStorageClient
import com.weightagent.app.data.cos.CosRepository
import com.weightagent.app.data.db.AppDatabase
import com.weightagent.app.data.mediastore.MediaStoreAudioScanner
import com.weightagent.app.data.mediastore.XiaomiPrivateRecorderScanner
import com.weightagent.app.data.saf.SafFolderStore
import com.weightagent.app.data.saf.SafTreeAudioScanner
import com.weightagent.app.data.settings.CloudStorageSelectionStore
import com.weightagent.app.data.settings.CosSettingsStore
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {

    val applicationContext = context.applicationContext

    val database: AppDatabase = AppDatabase.build(applicationContext)
    val recordingDao = database.recordingDao()
    val cosSettingsStore = CosSettingsStore(applicationContext)
    val cloudStorageSelectionStore = CloudStorageSelectionStore(applicationContext, cosSettingsStore)
    val aliyunDriveSettingsStore = AliyunDriveSettingsStore(applicationContext)
    val mediaStoreScanner = MediaStoreAudioScanner(applicationContext, recordingDao)
    val xiaomiPrivateRecorderScanner = XiaomiPrivateRecorderScanner(applicationContext, recordingDao)
    val safFolderStore = SafFolderStore(applicationContext)
    val safTreeAudioScanner = SafTreeAudioScanner(applicationContext, recordingDao, safFolderStore)
    val cosRepository = CosRepository(applicationContext)
    val cosObjectStorageClient = CosObjectStorageClient(applicationContext, cosSettingsStore, cosRepository)
    val aliyunDriveRepository = AliyunDriveRepository()
    val aliyunDriveConsumerClient = AliyunDriveConsumerClient(aliyunDriveSettingsStore, aliyunDriveRepository)
    val cloudUploadGateway = CloudUploadGateway(
        selectionStore = cloudStorageSelectionStore,
        cosSettingsStore = cosSettingsStore,
        aliyunDriveSettingsStore = aliyunDriveSettingsStore,
        cosObjectStorageClient = cosObjectStorageClient,
        aliyunDriveConsumerClient = aliyunDriveConsumerClient,
    )
    val workManager: WorkManager get() = WorkManager.getInstance(applicationContext)

    init {
        // 同步一次即可；避免 NavHost 首帧读到「未迁移」而误判为未选择
        runBlocking(Dispatchers.IO) {
            cloudStorageSelectionStore.migrateDefaultFromLegacyCosIfNeeded()
        }
    }
}
