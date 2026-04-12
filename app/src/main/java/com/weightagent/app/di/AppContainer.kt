package com.weightagent.app.di

import android.content.Context
import androidx.work.WorkManager
import com.weightagent.app.data.cos.CosRepository
import com.weightagent.app.data.db.AppDatabase
import com.weightagent.app.data.mediastore.MediaStoreAudioScanner
import com.weightagent.app.data.mediastore.XiaomiPrivateRecorderScanner
import com.weightagent.app.data.saf.SafFolderStore
import com.weightagent.app.data.saf.SafTreeAudioScanner
import com.weightagent.app.data.settings.CosSettingsStore

class AppContainer(context: Context) {

    val applicationContext = context.applicationContext

    val database: AppDatabase = AppDatabase.build(applicationContext)
    val recordingDao = database.recordingDao()
    val cosSettingsStore = CosSettingsStore(applicationContext)
    val mediaStoreScanner = MediaStoreAudioScanner(applicationContext, recordingDao)
    val xiaomiPrivateRecorderScanner = XiaomiPrivateRecorderScanner(applicationContext, recordingDao)
    val safFolderStore = SafFolderStore(applicationContext)
    val safTreeAudioScanner = SafTreeAudioScanner(applicationContext, recordingDao, safFolderStore)
    val cosRepository = CosRepository(applicationContext)
    val workManager: WorkManager get() = WorkManager.getInstance(applicationContext)
}
