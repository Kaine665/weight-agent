package com.weightagent.app.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.weightagent.app.data.db.RecordingDeduper
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.di.AppContainer
import com.weightagent.app.work.RefreshAndEnqueueWorker
import com.weightagent.app.work.UploadRecordingWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingListViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val recordings = container.recordingDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val safTreeUriStrings = container.safFolderStore.treeUriStrings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val isRefreshing = MutableStateFlow(false)

    private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessages = _uiMessages.asSharedFlow()

    private fun postMessage(msg: String) {
        viewModelScope.launch { _uiMessages.emit(msg) }
    }

    /**
     * @param showUserFeedback 为 true 时在成功/失败时弹出 Snackbar（避免仅进入页面时的静默刷新打扰用户）
     */
    fun refresh(showUserFeedback: Boolean = false) {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                container.mediaStoreScanner.refreshFromMediaStore()
                container.xiaomiPrivateRecorderScanner.scanIfApplicable()
                container.safTreeAudioScanner.scanPersistedTrees()
                RecordingDeduper.dedupeAfterScan(container.recordingDao)
                container.workManager.enqueueUniqueWork(
                    RefreshAndEnqueueWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.KEEP,
                    RefreshAndEnqueueWorker.buildRequest(),
                )
                if (showUserFeedback) {
                    postMessage("列表已更新")
                }
            } catch (t: Throwable) {
                postMessage(
                    "扫描失败：${t.message?.trim().orEmpty().ifBlank { t.javaClass.simpleName }}",
                )
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun addSafTreeFolder(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                container.applicationContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            container.safFolderStore.add(uri.toString())
            postMessage("已添加扫描目录")
            refresh()
        }
    }

    fun removeSafTreeFolder(uriString: String) {
        viewModelScope.launch {
            runCatching {
                val u = Uri.parse(uriString)
                container.applicationContext.contentResolver.releasePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            container.safFolderStore.remove(uriString)
            postMessage("已移除扫描目录")
            refresh()
        }
    }

    fun enqueueUpload(mediaStoreId: Long) {
        viewModelScope.launch {
            val settings = container.cosSettingsStore.read()
            if (settings == null || !settings.isComplete()) {
                _uiMessages.emit("请先在右上角「COS 配置」中填写并保存 SecretId、SecretKey、地域与存储桶后再上传")
                return@launch
            }
            val row = container.recordingDao.getById(mediaStoreId)
            if (row == null) {
                _uiMessages.emit("找不到该录音条目")
                return@launch
            }
            if (row.syncStatus == SyncStatus.UPLOADING) {
                _uiMessages.emit("该文件正在上传中")
                return@launch
            }
            val req = UploadRecordingWorker.buildRequest(mediaStoreId)
            container.workManager.enqueueUniqueWork(
                UploadRecordingWorker.uniqueWorkName(mediaStoreId),
                ExistingWorkPolicy.REPLACE,
                req,
            )
            _uiMessages.emit("已加入上传队列")
        }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecordingListViewModel(container) as T
    }
}
