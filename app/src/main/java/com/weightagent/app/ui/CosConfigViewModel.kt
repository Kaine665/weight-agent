package com.weightagent.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.weightagent.app.data.cos.CosRepository
import com.weightagent.app.data.settings.CosSettings
import com.weightagent.app.data.settings.CosSettingsStore
import com.weightagent.app.di.AppContainer
import com.weightagent.app.work.RefreshAndEnqueueWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CosConfigUiState(
    val secretId: String = "",
    val secretKey: String = "",
    val region: String = "",
    val bucket: String = "",
    val prefix: String = "recordings/",
    val message: String? = null,
    val isBusy: Boolean = false,
)

class CosConfigViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val store: CosSettingsStore = container.cosSettingsStore
    private val cosRepository: CosRepository = container.cosRepository

    private val _ui = MutableStateFlow(CosConfigUiState())
    val ui: StateFlow<CosConfigUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = store.read()
            if (saved != null) {
                _ui.value = CosConfigUiState(
                    secretId = saved.secretId,
                    secretKey = saved.secretKey,
                    region = saved.region,
                    bucket = saved.bucket,
                    prefix = saved.prefix,
                )
            }
        }
    }

    fun updateSecretId(v: String) {
        _ui.value = _ui.value.copy(secretId = v, message = null)
    }

    fun updateSecretKey(v: String) {
        _ui.value = _ui.value.copy(secretKey = v, message = null)
    }

    fun updateRegion(v: String) {
        _ui.value = _ui.value.copy(region = v, message = null)
    }

    fun updateBucket(v: String) {
        _ui.value = _ui.value.copy(bucket = v, message = null)
    }

    fun updatePrefix(v: String) {
        _ui.value = _ui.value.copy(prefix = v, message = null)
    }

    fun save() {
        val s = _ui.value.toSettingsOrNull() ?: run {
            _ui.value = _ui.value.copy(message = "请填写 SecretId、SecretKey、region、bucket")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isBusy = true, message = null)
            try {
                store.save(s)
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
        val s = _ui.value.toSettingsOrNull() ?: run {
            _ui.value = _ui.value.copy(message = "请填写 SecretId、SecretKey、region、bucket 后再测试")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isBusy = true, message = null)
            try {
                cosRepository.headBucket(s)
                _ui.value = _ui.value.copy(message = "测试连接成功", isBusy = false)
            } catch (t: Throwable) {
                val msg = t.message?.trim().orEmpty()
                _ui.value = _ui.value.copy(
                    message = if (msg.isNotBlank()) "测试失败：$msg" else "测试失败，请检查网络与参数",
                    isBusy = false,
                )
            }
        }
    }

    private fun CosConfigUiState.toSettingsOrNull(): CosSettings? {
        val s = CosSettings(
            secretId = secretId.trim(),
            secretKey = secretKey.trim(),
            region = region.trim(),
            bucket = bucket.trim(),
            prefix = prefix.trim().ifBlank { "recordings/" },
        )
        return if (s.isComplete()) s else null
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CosConfigViewModel(container) as T
    }
}
