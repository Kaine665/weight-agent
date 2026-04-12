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
./gradlew :app:assembleRelease  # 编译 Release APK（无 keystore.properties 时使用 debug 签名）
./gradlew :app:lintDebug        # Lint（需已配置 SDK）
./gradlew :app:testDebugUnitTest # 单元测试（若有）
```

### 服务与运行

- 无需单独启动后端服务；应用直连 **腾讯云 COS**。
- 使用 Android Studio 打开仓库根目录，选择 `app` 运行到 **API 34+** 真机或模拟器。

### 注意事项

- 本地若未安装 Android SDK，`assemble` 会因缺少 `sdk.dir` 失败；配置 `local.properties` 或使用 Android Studio 即可。
- **GitHub Actions**：`.github/workflows/android-build.yml` 使用 `android-actions/setup-android@v3` 安装 SDK，并上传 **Debug 与 Release** APK 构件；克隆仓库后也可在本地按上文自行配置 SDK。
- 密钥仅保存在设备加密存储中，勿将含真实密钥的 `local.properties` 或其他文件提交到 Git。
