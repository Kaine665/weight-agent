# weight-agent

## Cursor Cloud / 代理说明

本仓库包含 **Android 应用模块**（`app/`）与产品规格文档（`docs/`）。

### 依赖与安装

- 在本机安装 **Android Studio**（含 Android SDK、Platform 34/35、Build-Tools）。
- 复制 `local.properties.example` 为 `local.properties`，将 `sdk.dir` 设为本机 SDK 路径（该文件勿提交）。
- 云端代理环境可使用 `sdkmanager` 将 Command-line Tools 安装到例如 `/workspace/android-sdk`（已加入 `.gitignore`），再令 `sdk.dir` 指向该路径。

### 常用命令（在项目根目录 `/workspace`）

```bash
export ANDROID_HOME=/path/to/android-sdk   # 可选；与 sdk.dir 一致时便于 sdkmanager
./gradlew :app:assembleDebug    # 编译 Debug APK
./gradlew :app:assembleRelease  # 无 keystore.properties 时用 keystores/ci-debug.keystore（与 debug/CI 同签名）
./gradlew :app:lintDebug        # Lint（需已配置 SDK）
./gradlew :app:testDebugUnitTest # 单元测试（若有）
```

### 服务与运行

- 无需单独启动后端服务；上传仅 **腾讯云 COS**，直连 `CosRepository`。
- **MCP**：根目录 **`mcp/cos-recordings/`** 为 Python stdio MCP（`cos-python-sdk-v5`），供 Cursor 等列举桶内对象、生成预签名 URL；**给人/给 Agent 的步骤与链接见 `docs/MCP_COS.md`**；`.cursor/mcp.json` 已加入 `.gitignore`，示例见 **`.cursor/mcp.json.example`**。
- 使用 Android Studio 打开仓库根目录，选择 `app` 运行到 **API 34+** 真机或模拟器。

### 注意事项

- 本地若未安装 Android SDK，`assemble` 会因缺少 `sdk.dir` 失败；配置 `local.properties` 或使用 Android Studio 即可。
- **Android CI 并发**：`android-build.yml` 的 `concurrency.group` 对 **push 到 main** 使用 **`github.sha`**（每次提交独立），且 **`cancel-in-progress` 仅对 PR 为 true**。若对 main 使用 `refs/heads/main` 作组键，合并后的 CI 会被紧随其后的 **`[skip ci]`** 版本提交取消，导致 **Release 发布工作流不触发**。
- **SAF 扫描目录**：列表页「扫描目录」用 `OpenDocumentTree` 持久化授权多个文件夹，`SafTreeAudioScanner` 递归写入 Room；依赖 **`androidx.documentfile:documentfile`**。
- **GitHub Actions**：`android-build.yml` 在 push/PR 到 `main` 时上传 **Artifacts**；在 **push 到 `main`**（且提交不含 `[skip ci]`）时先 **`scripts/bump_app_version.py`** 自增版本并回推 `main`；**`android-publish-latest.yml`**（`Publish main APK (per version)`）为 **`v{versionName}`** 各建 **独立 Release**（`weight-agent-release.apk`），**不再覆盖旧版 `app-latest`**。`android-release.yml` 在推送 **`v*`** 标签时另建 Release（`app-release.apk`）。
- 工作流使用 **Node.js 24** 系 Actions（如 `actions/checkout@v5`、`setup-java@v5`、`cache@v5`、`upload-artifact@v6`、`download-artifact@v8`、`android-actions/setup-android@v4`、`softprops/action-gh-release@v3`），并设置 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`，以避免 GitHub 对 Node 20 运行时的弃用告警。
- 密钥仅保存在设备加密存储中，勿将含真实密钥的 `local.properties` 或其他文件提交到 Git。
