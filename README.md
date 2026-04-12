# weight-agent

安卓本地录音同步至腾讯云 COS（第一期 MVP）。产品验收与边界见 **`docs/SPEC.md`**；Android 工程位于 **`app/`**。

### 安装包在哪下（和 GitHub 主页的关系）

GitHub **仓库主页**右侧（或顶部导航里的）**「Releases」**，对应的是 **[Releases 页面](https://github.com/Kaine665/weight-agent/releases)** 里的 **GitHub Release**（带版本号、可挂附件），**不是** Actions 里某次运行的 **Artifacts**。本仓库用 CI 自动创建/更新 **`app-latest`** 这条 Release：**标题为当次构建的软件版本号**（如 **`v0.1.5`**，与 `app/build.gradle.kts` 的 `versionName` 一致），附件 **`weight-agent-release.apk`**。合并到 `main` 并跑绿 CI 后即可在 Releases 页下载。若仍为空，说明尚未有一次成功的 **`Publish Latest (main)`**；**Actions 里的 Artifacts** 不会单独出现在主页 Releases 栏。

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
2. 复制 `local.properties.example` 为 `local.properties`，填写 `sdk.dir`（指向本机 Android SDK；**勿提交**该文件）。
3. 在项目根目录执行：  
   - Debug：`./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`  
   - Release：`./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`  
4. **签名（避免「与已安装应用签名不同」）**：**debug** 与**未配置正式 keystore 时的 release** 均使用仓库内 **`keystores/ci-debug.keystore`**（与 Android 默认 debug 口令一致：`android` / `androiddebugkey`），与 **GitHub Actions 打出的 APK 同签名**，可直接覆盖安装。若你手机里已是**以前用本机默认 debug.keystore 装的旧包**，请先**卸载**再装。正式分发请配置 **`keystore.properties` + `release.keystore`**（勿提交私钥）。  
5. **版本号**：平时开发可手动改 `versionCode` / `versionName`；**`main` 上 CI 成功后会自动 patch 自增**（见上文），本地拉取 `main` 即可获得与 Releases 一致的版本。

在无 Android Studio 的环境（如 CI）中，也可将 SDK 解压到仓库旁的自定义目录，并在 `local.properties` 中写 `sdk.dir=/绝对路径`；本仓库 `.gitignore` 已忽略常见本地下载目录名 `android-sdk/`。

### GitHub Actions

对 `main` 的 **push**/**pull_request** 会触发 **Android CI**（见 `.github/workflows/android-build.yml`）：安装 SDK、执行 `./gradlew :app:assembleDebug`、`:app:assembleRelease` 与 `:app:testDebugUnitTest`，并将构件 **`app-debug-apk`** 与 **`app-release-apk`** 上传到该次运行的 **Artifacts**（路径：**Actions** → 点开某次运行 → 页面底部的 **Artifacts**）。未配置 `release.keystore` 时，CI 的 release 与 **debug 同为 `ci-debug.keystore` 签名**；若要在 CI 使用正式证书，请配置 `ANDROID_KEYSTORE_*` 等。

**GitHub Releases（右侧「Releases」页）**：

- **默认「main 滚动发布」**：每次 **push 到 `main`**（且提交说明不含 **`[skip ci]`**）时，**Android CI** 会在构建前将 **`versionCode` +1**、**`versionName` 的 patch +1**（如 `0.1.4` → `0.1.5`），构建成功后 **回推到 `main`**（提交信息含 `[skip ci]`，避免无限循环）。随后 **`.github/workflows/android-publish-latest.yml`** 更新 **`app-latest`** Release，**标题为 `v{versionName}`**。PR 的 CI 不 bump、不推送。详见脚本 **`scripts/bump_app_version.py`**。
- **版本号发布**：推送形如 **`v0.1.1`** 的 **git tag** 会触发 **`.github/workflows/android-release.yml`**，另建一条带版本名的 Release 并附上 **`app-release.apk`**。示例：

```bash
git tag v0.1.1
git push origin v0.1.1
```

### 权限

- **`READ_MEDIA_AUDIO`**：从 `MediaStore` 读取系统可见的音频（含录音机产物）。
- **`MANAGE_EXTERNAL_STORAGE`（全部文件访问）**：声明于清单；**仅小米/红米/POCO** 且在 **Android 11+** 上，若系统录音保存在 **`Android/data/.../录音机`** 等私有目录、未进媒体库，需用户在系统设置中为本应用开启「全部文件访问权限」后，应用才能扫描这些目录（列表页会显示引导按钮）。非小米设备或非该场景可不授予。
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
- 以 **MediaStore 可见**音频为主；**小米系**在授予 **全部文件访问** 后，会额外扫描 **`Android/data/com.android.soundrecorder`**、**`com.miui.soundrecorder`** 下常见录音扩展名（系统录音机私有目录）。扫描 **Audio.Media** 与 **Files（MEDIA_TYPE_AUDIO）**；**IS_PENDING** 接受 **NULL** 与 `0`；不按 MIME 在 SQL 里限制为 `audio/*`；跳过 `video/*`。上传支持 **`content://`** 与 **`file://`**。
- 断网、杀进程依赖 **WorkManager** 重试与用户再次打开 App 触发巡检；不承诺秒级上传。
- 清除应用数据会丢失本地配置与队列状态；云端已存在对象**不会**自动与本地对齐（SPEC 用例 10）。

---

## 文档索引

| 路径 | 说明 |
|------|------|
| [docs/SPEC.md](docs/SPEC.md) | 第一期验收、页面结构、同步原则、DoD、10 条验收用例 |
| [docs/PROJECT_ORIGIN.md](docs/PROJECT_ORIGIN.md) | 项目来源说明 |
| [AGENTS.md](AGENTS.md) | 代理/云端构建说明 |
