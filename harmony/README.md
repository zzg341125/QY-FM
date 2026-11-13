# QY·FM 原生鸿蒙版

这是 QY·FM 的独立 HarmonyOS ArkTS / ArkUI 工程。原 Android 项目与 APK 发布不在此目录内修改。

## 当前能力

- 原生 AVPlayer 网络电台播放与 AVSession 锁屏播控。
- 原生 A2DP 蓝牙连接监听；同一设备断开后 120 分钟内重连自动续播。
- 蓝牙断开后使用低开销静音音频流维持长时任务，避免系统在约 1 分钟后停止监听。
- 电台新增、编辑、删除和上下顺序持久化。
- 只有打开“编辑电台”后才显示删除入口。
- WebDAV 上传与恢复，备份 JSON 中包含电台、台标数据和明确顺序。
- GitHub 原生版版本检查、AppGallery 系统升级入口、手动检查。
- 播放页与设置页使用原生虚拟化列表和弹性滚动。

## 重要系统约束

HarmonyOS 不允许普通第三方应用静默安装自身。在线升级流程负责检测并展示系统升级框，或跳转到 GitHub 发布页下载 HAP；最终安装由 HarmonyOS 系统安装器确认。

原生 A2DP 回调在应用进程存活时可靠工作。为了覆盖用户要求的 120 分钟等待窗口，本项目在暂停真实电台后启动一条静音音频保持流。它会带来少量持续功耗，可在设置中关闭“蓝牙重连自动续播”。

## 工具链

需要 DevEco Studio 6.1+ 或 HarmonyOS Command Line Tools 26+，并安装 API 23 SDK。

```powershell
devecocli build
devecocli run
```

签名需要先执行：

```powershell
devecocli auth login
devecocli signature generate --product default
```

## 发布约定

- Android 继续使用 `v1.x.x` 标签和 APK。
- HarmonyOS 原生版使用 `harmony-v1.x.x` 标签和 HAP。
- 不覆盖或删除 Android `v1.0.9` 的 APK。

详细实现与真机验收说明见 [docs/HARMONYOS_NATIVE_MIGRATION.md](docs/HARMONYOS_NATIVE_MIGRATION.md)。
