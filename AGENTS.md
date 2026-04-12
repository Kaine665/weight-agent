# weight-agent

## Cursor Cloud / 代理说明

本仓库包含 **Android 应用模块**（`app/`）与产品规格文档（`docs/`）。

### 依赖与安装

- 在本机安装 **Android Studio**（含 Android SDK、Platform 34/35、Build-Tools）。
- 复制 `local.properties.example` 为 `local.properties`，将 `sdk.dir` 设为本机 SDK 路径（该文件勿提交）。

### 常用命令（在项目根目录 `/workspace`）

```bash
./gradlew :app:assembleDebug    # 编译 Debug APK
./gradlew :app:lintDebug        # Lint（需已配置 SDK）
./gradlew :app:testDebugUnitTest # 单元测试（若有）
```

### 服务与运行

- 无需单独启动后端服务；应用直连 **腾讯云 COS**。
- 使用 Android Studio 打开仓库根目录，选择 `app` 运行到 **API 34+** 真机或模拟器。

### 注意事项

- CI/云端环境若未安装 Android SDK，`assemble` 会因缺少 `sdk.dir` 失败；在具备 SDK 的机器上构建即可。
- 密钥仅保存在设备加密存储中，勿将含真实密钥的 `local.properties` 或其他文件提交到 Git。
