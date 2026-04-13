package com.weightagent.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudHubScreen(
    navController: NavController,
    viewModel: CloudHubViewModel = viewModel(
        factory = CloudHubViewModel.Factory(
            (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container,
        ),
    ),
) {
    val kind by viewModel.selectedKind.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("云端与上传") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
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
                when (kind) {
                    CloudStorageKind.OBJECT_COS -> "当前上传方式：对象存储 · 腾讯云 COS"
                    CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> "当前上传方式：消费级网盘 · 阿里云盘"
                    null -> "尚未选择上传方式"
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = { navController.navigate(Routes.CONFIG_COS) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("腾讯云 COS 配置")
            }
            Button(
                onClick = { navController.navigate(Routes.CONFIG_ALIYUN_DRIVE) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("阿里云盘 配置")
            }
            OutlinedButton(
                onClick = {
                    viewModel.clearKindAndGoSelect {
                        navController.navigate(Routes.CLOUD_SELECT) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("更改上传方式（重新选择）")
            }
        }
    }
}
