# CI / 本地统一调试签名

`ci-debug.keystore` 用于 **debug** 与 **未配置 `release.keystore` 时的 release** 包签名，使：

- **Android Studio Run（debug）**、**本机 `./gradlew assembleRelease`** 与 **GitHub Actions 产出的 APK** 使用**同一签名**，可互相覆盖安装，不会出现「签名不一致」。

参数与 Android 默认 debug 一致（公开、仅适合自用 / 开源调试）：

| 项 | 值 |
|----|-----|
| Keystore 密码 | `android` |
| Key alias | `androiddebugkey` |
| Key 密码 | `android` |

正式上架或对外分发请使用 **`keystore.properties` + 自己的 `release.keystore`**（勿提交私钥仓库）。
