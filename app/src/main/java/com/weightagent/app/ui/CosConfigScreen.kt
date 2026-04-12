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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosConfigScreen(
    navController: NavController,
    viewModel: CosConfigViewModel = viewModel(
        factory = CosConfigViewModel.Factory(
            (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container,
        ),
    ),
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("COS 配置") },
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
                "请使用 CAM 子账号密钥，并收敛到目标桶最小权限。SecretKey 仅保存在本机加密存储，不会写入日志或 Git。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "提示：若手机丢失且未锁屏，桶凭证存在泄露风险；请自行评估。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            OutlinedTextField(
                value = ui.secretId,
                onValueChange = viewModel::updateSecretId,
                label = { Text("SecretId") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !ui.isBusy,
            )
            OutlinedTextField(
                value = ui.secretKey,
                onValueChange = viewModel::updateSecretKey,
                label = { Text("SecretKey") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !ui.isBusy,
            )
            OutlinedTextField(
                value = ui.region,
                onValueChange = viewModel::updateRegion,
                label = { Text("地域 region（如 ap-guangzhou）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !ui.isBusy,
            )
            OutlinedTextField(
                value = ui.bucket,
                onValueChange = viewModel::updateBucket,
                label = { Text("存储桶 bucket（含 AppId，如 mybucket-1250000000）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !ui.isBusy,
            )
            OutlinedTextField(
                value = ui.prefix,
                onValueChange = viewModel::updatePrefix,
                label = { Text("对象前缀 prefix") },
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
