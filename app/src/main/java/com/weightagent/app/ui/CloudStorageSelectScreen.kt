package com.weightagent.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudStorageSelectScreen(
    navController: NavController,
    viewModel: CloudStorageSelectViewModel = viewModel(
        factory = CloudStorageSelectViewModel.Factory(
            (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container,
        ),
    ),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择云端") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "请选择录音上传方式。可随时在录音列表右上角「云端」里更改。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    viewModel.selectKind(CloudStorageKind.OBJECT_COS) {
                        navController.navigate(Routes.CONFIG_COS) {
                            popUpTo(Routes.CLOUD_SELECT) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("对象存储 · 腾讯云 COS")
            }
            Button(
                onClick = {
                    viewModel.selectKind(CloudStorageKind.CONSUMER_ALIYUN_DRIVE) {
                        navController.navigate(Routes.CONFIG_ALIYUN_DRIVE) {
                            popUpTo(Routes.CLOUD_SELECT) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("消费级网盘 · 阿里云盘")
            }
        }
    }
}
