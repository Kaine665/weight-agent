package com.weightagent.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weightagent.app.ui.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AliyunDriveConfigScreen(
    navController: NavController,
    viewModel: AliyunDriveConfigViewModel = viewModel(
        factory = AliyunDriveConfigViewModel.Factory(
            (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container,
        ),
    ),
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.message) {
        if (ui.message == "已保存") {
            navController.navigate(Routes.LIST) {
                popUpTo(Routes.LIST) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阿里云盘") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "使用阿里云盘开放平台 refresh_token。令牌仅保存在本机加密存储；请勿分享给他人。若官方接口变更导致失败，请反馈或改用 COS。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedTextField(
                value = ui.refreshToken,
                onValueChange = viewModel::updateRefreshToken,
                label = { Text("refresh_token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !ui.isBusy,
            )
            OutlinedTextField(
                value = ui.remoteFolder,
                onValueChange = viewModel::updateRemoteFolder,
                label = { Text("网盘内保存目录（根下文件夹名）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !ui.isBusy,
            )
            Button(
                onClick = viewModel::testConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.isBusy,
            ) {
                Text(if (ui.isBusy) "请稍候…" else "测试连接")
            }
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !ui.isBusy,
            ) {
                Text("保存")
            }
            val msg = ui.message
            if (!msg.isNullOrBlank()) {
                Text(
                    msg,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (msg.startsWith("已保存") || msg.startsWith("测试连接成功")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}
