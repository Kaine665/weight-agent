package com.weightagent.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.weightagent.app.data.db.RecordingEntity
import com.weightagent.app.data.db.SyncStatus
import com.weightagent.app.data.device.OemDevice
import com.weightagent.app.data.saf.SafOpenHelper
import com.weightagent.app.data.storage.AllFilesAccessHelper
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
    val safFolders by viewModel.safTreeUriStrings.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()

    val openTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            viewModel.addSafTreeFolder(uri)
        }
    }

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

    var hasAllFilesAccess by remember {
        mutableStateOf(AllFilesAccessHelper.hasAllFilesAccess(context))
    }
    var storageRecheckKey by remember { mutableStateOf(0) }
    LaunchedEffect(hasAudioPermission, storageRecheckKey) {
        if (hasAudioPermission) {
            hasAllFilesAccess = AllFilesAccessHelper.hasAllFilesAccess(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllFilesAccess = AllFilesAccessHelper.hasAllFilesAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var showSafSheet by remember { mutableStateOf(false) }
    val safSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("录音列表") },
                actions = {
                    IconButton(
                        onClick = { showSafSheet = true },
                        enabled = hasAudioPermission,
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "扫描目录")
                    }
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

        val showXiaomiAllFilesBanner = OemDevice.isXiaomiFamily() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            hasAudioPermission &&
            !hasAllFilesAccess

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (showXiaomiAllFilesBanner) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "检测到小米/红米：新录音常在「Android/data/…」私有目录，未进媒体库。请开启「全部文件访问权限」后下拉刷新；否则只能扫公共目录（如 MIUI/sound_recorder）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                AllFilesAccessHelper.openManageAllFilesSettings(context)
                            },
                        ) {
                            Text("去开启全部文件访问权限")
                        }
                        OutlinedButton(
                            onClick = {
                                storageRecheckKey++
                                viewModel.refresh()
                            },
                        ) {
                            Text("我已授权，重新扫描")
                        }
                    }
                }
                if (recordings.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (showXiaomiAllFilesBanner) {
                            Text(
                                "当前未扫描到录音。请先完成上方「全部文件访问」再下拉刷新；若已开启仍为空，请到系统录音机里查看「保存路径」是否指向本机可访问目录。",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(
                                "暂无录音条目。请下拉刷新。已扫描媒体库与常见文件夹（含 MIUI/sound_recorder）；若仍无，请确认系统「文件」→「音频」里是否能看到该录音。",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = true),
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

    if (hasAudioPermission && showSafSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSafSheet = false },
            sheetState = safSheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "扫描目录（文件管理器）",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "用系统文件管理器授权一个或多个公共文件夹；Android/data 等路径系统不允许在此授权。移除仅取消本应用授权，不会删除手机里的文件。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { openTreeLauncher.launch(SafOpenHelper.initialTreeUriForPicker()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("添加目录", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (safFolders.isNotEmpty()) {
                    Text(
                        "已添加 ${safFolders.size} 个目录",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    safFolders.forEach { uriString ->
                        key(uriString) {
                            val label = runCatching {
                                val u = Uri.parse(uriString)
                                u.lastPathSegment ?: uriString
                            }.getOrDefault(uriString)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                )
                                IconButton(
                                    onClick = { viewModel.removeSafTreeFolder(uriString) },
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "移除目录",
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { showSafSheet = false },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("完成")
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
