package com.weightagent.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.di.AppContainer
import kotlinx.coroutines.launch

class CloudStorageSelectViewModel(
    private val container: AppContainer,
) : ViewModel() {

    fun selectKind(kind: CloudStorageKind, onSaved: () -> Unit) {
        viewModelScope.launch {
            container.cloudStorageSelectionStore.saveKind(kind)
            onSaved()
        }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CloudStorageSelectViewModel(container) as T
    }
}
