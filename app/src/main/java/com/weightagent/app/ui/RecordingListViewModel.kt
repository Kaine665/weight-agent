package com.weightagent.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.weightagent.app.di.AppContainer
import com.weightagent.app.work.RefreshAndEnqueueWorker
import com.weightagent.app.work.UploadRecordingWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingListViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val recordings = container.recordingDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isRefreshing = MutableStateFlow(false)

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                container.mediaStoreScanner.refreshFromMediaStore()
                container.workManager.enqueueUniqueWork(
                    RefreshAndEnqueueWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.KEEP,
                    RefreshAndEnqueueWorker.buildRequest(),
                )
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun enqueueUpload(mediaStoreId: Long) {
        val req = UploadRecordingWorker.buildRequest(mediaStoreId)
        container.workManager.enqueueUniqueWork(
            UploadRecordingWorker.uniqueWorkName(mediaStoreId),
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecordingListViewModel(container) as T
    }
}
