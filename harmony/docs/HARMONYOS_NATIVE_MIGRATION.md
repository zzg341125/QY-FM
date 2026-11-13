# QY·FM 原生鸿蒙迁移说明

## 目标

保留 Android APK 项目、标签和发布资产，新增独立 HarmonyOS 原生工程。原生版必须解决 APK 兼容层在 HarmonyOS 7 上无法感知 A2DP 重连的问题。

## 架构

| 模块 | 文件 | 职责 |
| --- | --- | --- |
| 主界面 | `entry/src/main/ets/pages/Index.ets` | 播放、电台编辑、排序、设置、备份和升级 UI |
| 电台仓库 | `entry/src/main/ets/data/StationRepository.ets` | Preferences 持久化、顺序恢复、JSON 备份 |
| 播放控制 | `entry/src/main/ets/services/RadioController.ets` | AVPlayer、AVSession、睡眠定时和蓝牙续播调度 |
| 蓝牙策略 | `entry/src/main/ets/services/BluetoothResumeManager.ets` | A2DP 状态、设备身份、120 分钟窗口 |
| 后台任务 | `entry/src/main/ets/services/BackgroundTaskService.ets` | 音频长时任务 |
| 静音保持 | `entry/src/main/ets/services/SilentAudioKeepAlive.ets` | 断开等待期间维持音频任务有效 |
| WebDAV | `entry/src/main/ets/services/WebDavBackupService.ets` | Basic Auth、PUT 上传、GET 恢复 |
| 在线升级 | `entry/src/main/ets/services/AppUpdateService.ets` | `harmony-v*` GitHub Release 与 AppGallery 更新 |

## 蓝牙续播规则

1. 用户手动开始播放，且 A2DP 设备处于连接状态时，记录当前设备地址。
2. 当前设备断开或音频输出回落到扬声器时，立即暂停真实电台。
3. 保存断开设备身份与时间戳，开始静音音频保持流。
4. 只有同一设备地址重新进入 `STATE_CONNECTED` 且间隔不超过 120 分钟才续播。
5. 手动暂停、停止、切换电台、睡眠定时停止都会取消待续播状态。
6. 正常启动应用不会自动播放；只有断连前正在播放的同一蓝牙设备重连才触发。

设备地址使用 HarmonyOS 返回的虚拟 MAC。文档说明已配对设备在蓝牙开关不重启时地址保持稳定，适合本次重连窗口。

## 为什么保留静音音频流

官方 FAQ 明确说明：

- 音频长时任务在暂停且没有实际播放时会被系统管控。
- 蓝牙长时任务在蓝牙断开后要求一分钟内重连，否则自动结束。

因此，仅创建蓝牙长时任务无法满足 120 分钟等待。项目只申请音频长时任务来避免“蓝牙断开一分钟即结束”的限制，并在真实电台暂停后启动 8 kHz 单声道静音 PCM，保持音频任务有实际渲染活动，以便锁屏状态下继续接收 A2DP 回调。超过 120 分钟、手动停止或关闭自动续播时会立即释放。

## 备份格式

WebDAV 保存 `application/json` 文件，`schemaVersion` 为 `2`：

```json
{
  "schemaVersion": 2,
  "app": "QYFM HarmonyOS",
  "appVersion": "1.0.0",
  "exportedAt": "ISO-8601",
  "stationOrder": ["id-2", "id-1"],
  "stations": [
    {
      "id": "id-2",
      "name": "电台名称",
      "streamUrl": "https://example/stream.m3u8",
      "iconUrl": "data:image/png;base64,...",
      "tags": "新闻",
      "country": "中国",
      "state": "河北",
      "favorite": true
    }
  ],
  "settings": {
    "autoUpdate": true,
    "autoResume": true,
    "resumeWindowMinutes": 120,
    "webdavUrl": "https://dav.example/radio/",
    "webdavUser": "user",
    "webdavFile": "qyfm-backup.json"
  }
}
```

用户选择的本地台标会转换为 Base64 data URL，因此不会因设备路径不同而丢失。`stationOrder` 是恢复顺序的唯一来源；缺失或新增的电台会去重后追加，避免恢复时顺序错乱。WebDAV 密码不会写入备份。

## 在线升级

- 启动时可自动检查 GitHub 中 `harmony-v*` 标签的最新稳定发布。
- 设置页提供手动检查。
- GitHub 发布存在时显示版本说明并跳转发布页。
- 若应用由 AppGallery 分发，同时调用 `updateManager.checkAppUpdate()` 与系统升级框。
- 普通第三方应用没有静默自安装权限，HAP 安装必须由系统确认。

## 真机验收

设备：HUAWEI Mate 80 Pro Max，HarmonyOS `7.0.0.111 SP12`。

1. 正常启动应用，确认没有自动播放。
2. 连接蓝牙耳机，播放电台，确认 A2DP 状态写入诊断信息。
3. 锁屏，关闭蓝牙耳机。
4. 确认电台暂停，应用显示“等待同一设备重连”。
5. 等待至少 2 分钟，再重新打开同一耳机。
6. 不解锁、不打开应用，确认原电台自动续播。
7. 等待超过 120 分钟后重连，确认不会自动续播。
8. 用设备 B 重连，确认不会误续播设备 A 的内容。
9. 播放时手动暂停再断开/重连，确认不会自动续播。
10. 调整电台顺序，备份到 WebDAV，删除或打乱本地顺序后恢复，确认顺序与台标一致。
11. 关闭编辑模式左滑，确认不出现删除按钮；打开编辑模式后确认可以删除。
12. 在播放页和设置页连续上下滑动，确认无明显掉帧和卡顿。

## 发布前仍需完成

- 安装 DevEco Studio/Command Line Tools 与 API 23 SDK。
- 完成 ArkTS 编译和 Code Linter。
- 使用华为账号生成 HAP 签名。
- 在真机执行上面的蓝牙锁屏验收。
- 生成 `QYFM-harmony-v1.0.0.hap`，创建 `harmony-v1.0.0` Release。
