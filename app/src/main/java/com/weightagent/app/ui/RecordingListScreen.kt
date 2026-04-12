package com.weightagent.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.ui.nav.Routes
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingListScreen(
    navController: NavController,
    viewModel: RecordingListViewModel = viewModel(
        factory = RecordingListViewModel.Factory(
            (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container,
        ),
    ),
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refresh()
        }
    }

    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录音列表") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = hasAudioPermission) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = { navController.navigate(Routes.CONFIG) }) {
                        Icon(Icons.Default.Settings, contentDescription = "COS 配置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        if (!hasAudioPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "需要「读取音频」权限，才能从系统媒体库加载录音机产生的文件。",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "请在下方授权；若系统未弹出对话框，请到系统设置中为本应用开启音频权限。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Button(
                    onClick = {
                        launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                    },
                ) {
                    Text("请求读取音频权限")
                }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (recordings.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Text(
                        "暂无录音条目。请确认已授权「读取音频」；在系统「文件」或「录音机」里能看到录音后，下拉刷新。若仍为空，可能是厂商把录音放在仅本机可见的目录（未进入媒体库），本应用只扫描 MediaStore。",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recordings, key = { it.mediaStoreId }) { row ->
                        RecordingRow(
                            row = row,
                            onUpload = { viewModel.enqueueUpload(row.mediaStoreId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    row: RecordingEntity,
    onUpload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                row.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "时长：${formatDuration(row.durationMs)}  ·  修改：${formatTime(row.dateModifiedMs)}  ·  大小：${formatSize(row.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "同步状态：${syncStatusLabel(row.syncStatus)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!row.lastError.isNullOrBlank()) {
                Text(
                    row.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (row.syncStatus != SyncStatus.SYNCED) {
                OutlinedButton(onClick = onUpload, enabled = row.syncStatus != SyncStatus.UPLOADING) {
                    Text(
                        when (row.syncStatus) {
                            SyncStatus.FAILED -> "重试上传"
                            else -> "立即上传"
                        },
                    )
                }
            }
        }
    }
}

private fun syncStatusLabel(s: SyncStatus): String = when (s) {
    SyncStatus.PENDING -> "未同步"
    SyncStatus.UPLOADING -> "上传中"
    SyncStatus.SYNCED -> "已同步"
    SyncStatus.FAILED -> "失败（可重试）"
    SyncStatus.PAUSED -> "已暂停"
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "—"
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
    return sdf.format(java.util.Date(ms))
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val kb = bytes / 1024.0
    return if (kb < 1024) String.format(LocaleChina, "%.1f KB", kb)
    else String.format(LocaleChina, "%.2f MB", kb / 1024.0)
}

private val LocaleChina = java.util.Locale.CHINA
