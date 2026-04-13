package com.weightagent.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weightagent.app.di.AppContainer
import kotlinx.coroutines.launch

class CloudHubViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val selectedKind = container.cloudStorageSelectionStore.selectedKind

    fun clearKindAndGoSelect(onCleared: () -> Unit) {
        viewModelScope.launch {
            container.cloudStorageSelectionStore.clearKind()
            onCleared()
        }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CloudHubViewModel(container) as T
    }
}
