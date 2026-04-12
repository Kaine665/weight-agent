# weight-agent

安卓本地录音同步至腾讯云 COS（第一期 MVP）。产品验收与边界见 **`docs/SPEC.md`**；Android 工程位于 **`app/`**。

## 自用场景约定（与规格书一致）

- **谁用、怎么装**：仅本人使用，不向他人分发。在本机用 Android Studio 打开工程并运行，或执行 `./gradlew :app:assembleDebug` 后用 `adb install` 安装生成的 APK。
- **设备**：单机自用验收即可。
- **COS 与桶内对象**：单一测试桶；对象可长期留在桶内，第一期不要求生命周期或定期清理。
- **状态与通知**：同步状态与队列巡检均在**应用内**展示；**不做系统通知栏上的业务提醒**（见 `docs/SPEC.md` §2.5）。若系统对后台任务强制前台服务通知，以平台行为为准。

---

## Android 工程说明

| 项 | 值 |
|----|-----|
| **应用模块** | `app` |
| **applicationId** | `com.weightagent.app` |
| **minSdk** | 34（Android 14+） |
| **targetSdk / compileSdk** | 35 |
| **UI** | Jetpack Compose + Material3 |
| **本地存储** | Room（录音与同步状态）+ `EncryptedSharedPreferences`（COS 密钥） |
| **后台上传** | WorkManager（`RefreshAndEnqueueWorker` 扫描并入队；`UploadRecordingWorker` 单条上传） |
| **COS SDK** | `com.qcloud.cos:cos-android-nobeacon:5.9.50`（固定密钥调试路径，见腾讯云文档） |

### 本机构建

1. 安装 Android Studio 与 SDK（含 API 34+）。
2. 复制 `local.properties.example` 为 `local.properties`，填写 `sdk.dir`。
3. 在项目根目录执行：`./gradlew :app:assembleDebug`  
4. APK 路径：`app/build/outputs/apk/debug/app-debug.apk`

### 权限

- **`READ_MEDIA_AUDIO`**：从 `MediaStore` 读取系统可见的音频（含录音机产物）。
- **`INTERNET`**、**`ACCESS_NETWORK_STATE`**：访问 COS。

首次启动若未授权，列表页会显示中文引导；授权后下拉刷新可重新扫描。

### 如何填写 COS

1. 打开应用 → 右上角 **设置** 进入 **COS 配置**。
2. 填写 **SecretId**、**SecretKey**（建议使用 CAM 子账号）、**region**（如 `ap-guangzhou`）、**bucket**（形如 `mybucket-1250000000`）、**prefix**（默认 `recordings/`）。
3. 先点 **测试连接**（`HeadBucket`），再点 **保存**。SecretKey 仅存本机加密存储，不会写入日志或 Git。
4. 保存后会自动触发一次扫描与入队；亦可在列表对单条点 **立即上传 / 重试**。

### 上传与对象键（幂等）

- 每条录音在首次扫描时生成稳定 **`recording_id`（UUID）**，并写入 Room。
- 对象键默认：`{prefix}{uuid}_{原始显示名}`（非法路径字符会替换为 `_`）；**失败重试沿用同一 `object_key`**，避免同一录音在云端产生多个无关联对象（整体重传覆盖同 key）。详见 `UploadRecordingWorker`。

### 已知限制（与 SPEC 非目标一致）

- 仅 **腾讯云 COS**，无多云。
- 以 **MediaStore 可见**音频为准，不保证抓取未进入媒体库的沙盒路径。
- 断网、杀进程依赖 **WorkManager** 重试与用户再次打开 App 触发巡检；不承诺秒级上传。
- 清除应用数据会丢失本地配置与队列状态；云端已存在对象**不会**自动与本地对齐（SPEC 用例 10）。

---

## 文档索引

| 路径 | 说明 |
|------|------|
| [docs/SPEC.md](docs/SPEC.md) | 第一期验收、页面结构、同步原则、DoD、10 条验收用例 |
| [docs/PROJECT_ORIGIN.md](docs/PROJECT_ORIGIN.md) | 项目来源说明 |
| [AGENTS.md](AGENTS.md) | 代理/云端构建说明 |
