package com.weightagent.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettings
import com.weightagent.app.di.AppContainer
import com.weightagent.app.work.RefreshAndEnqueueWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AliyunDriveConfigUiState(
    val refreshToken: String = "",
    val remoteFolder: String = "recordings",
    val message: String? = null,
    val isBusy: Boolean = false,
)

class AliyunDriveConfigViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _ui = MutableStateFlow(AliyunDriveConfigUiState())
    val ui: StateFlow<AliyunDriveConfigUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = container.aliyunDriveSettingsStore.read()
            if (saved != null) {
                _ui.value = _ui.value.copy(
                    refreshToken = saved.refreshToken,
                    remoteFolder = saved.normalizedRemoteFolder(),
                )
            }
        }
    }

    fun updateRefreshToken(v: String) {
        _ui.value = _ui.value.copy(refreshToken = v, message = null)
    }

    fun updateRemoteFolder(v: String) {
        _ui.value = _ui.value.copy(remoteFolder = v, message = null)
    }

    fun save() {
        val rt = _ui.value.refreshToken.trim()
        if (rt.isBlank()) {
            _ui.value = _ui.value.copy(message = "请填写 refresh_token")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isBusy = true, message = null)
            try {
                container.aliyunDriveSettingsStore.save(
                    AliyunDriveSettings(
                        refreshToken = rt,
                        remoteFolder = _ui.value.remoteFolder,
                    ),
                )
                container.cloudStorageSelectionStore.saveKind(CloudStorageKind.CONSUMER_ALIYUN_DRIVE)
                container.workManager.enqueueUniqueWork(
                    RefreshAndEnqueueWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.REPLACE,
                    RefreshAndEnqueueWorker.buildRequest(),
                )
                _ui.value = _ui.value.copy(message = "已保存", isBusy = false)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(message = "保存失败：${t.message}", isBusy = false)
            }
        }
    }

    fun testConnection() {
        val rt = _ui.value.refreshToken.trim()
        if (rt.isBlank()) {
            _ui.value = _ui.value.copy(message = "请先填写 refresh_token")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isBusy = true, message = null)
            try {
                container.aliyunDriveRepository.refreshSession(AliyunDriveSettings(refreshToken = rt))
                _ui.value = _ui.value.copy(message = "测试连接成功", isBusy = false)
            } catch (t: Throwable) {
                val msg = t.message?.trim().orEmpty()
                _ui.value = _ui.value.copy(
                    message = if (msg.isNotBlank()) "测试失败：$msg" else "测试失败，请检查网络与令牌",
                    isBusy = false,
                )
            }
        }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AliyunDriveConfigViewModel(container) as T
    }
}
