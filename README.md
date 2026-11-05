# 📻 收音机

一个极简风格的 Android 网络收音机：播放列表即首页，点开就听。

> 基于开源项目 **LX-FM**（Apache-2.0）修改，首页交互参考 **Transistor**。
> 完整许可见 [LICENSE](LICENSE)。

## 功能

- **首页播放列表**：台标 + 台名 + 圆形播放/停止按钮，无标题栏干扰
- **行内编辑**：长按电台直接在列表内改名、改流地址、换台标
- **拖动排序**：长按后上下拖动调整顺序，自动保存
- **左滑删除**：左滑露出红色删除区，二次确认
- **添加到桌面**：为单个电台生成桌面快捷方式，点击直接开播
- **睡眠定时**：长按底部播放条展开卡片，大按钮循环切换 `15 → 30 → 45 → 60 → 关闭`
- **在线备份 (WebDAV)**：播放列表、设置、台标打包上传；每次上传生成带时间戳的新备份，可从云端列表选择任意一版恢复
- **M3U 导入导出**：支持 `#EXTINF` 名称与 `tvg-logo` 台标
- **主题**：多套内置配色，支持系统强调色 / 透明 UI
- **稳定性**：HLS 直播缓冲优化、断流自愈、回到前台自动续播

## 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | 21（AGP 8.3 要求 17+） |
| Gradle | 8.7（`gradle/wrapper/gradle-wrapper.properties`） |
| Android SDK | platform 34 + build-tools 34.0.0 |
| minSdk / targetSdk | 26 / 34 |

## 本地构建

```bash
# 1. 配置 SDK 路径
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 2. 配置签名（见下节），然后构建
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

> 未配置签名时 `assembleRelease` 仍可执行，只是 APK 未签名、无法直接安装。
> 调试可用 `./gradlew assembleDebug`。

## 签名配置

在仓库根目录创建 `keystore.properties`（**已被 .gitignore 忽略，不会上传**）：

```properties
storeFile=../keystore/release.keystore
storePassword=你的store密码
keyAlias=你的别名
keyPassword=你的key密码
```

`app/build.gradle.kts` 也支持环境变量注入（适合 CI）：

| 环境变量 | 含义 |
| --- | --- |
| `KEYSTORE_FILE` | keystore 文件路径 |
| `KEYSTORE_PASSWORD` | store 密码 |
| `KEY_ALIAS` | key 别名 |
| `KEY_PASSWORD` | key 密码 |

## GitHub Actions 自动出包

工作流：`.github/workflows/android-release.yml`

- push 到 `main`：构建 APK 并上传为 Artifact
- push 形如 `v1.0.1` 的 tag：构建并自动创建 GitHub Release、附带 APK
- 也可在 Actions 页面手动 `Run workflow`

在仓库 **Settings → Secrets and variables → Actions** 的 **Secrets** 页签添加（放在 Variables 里也能被读到，但不推荐，Variables 是明文）：

| Secret | 说明 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore 转 base64，例如 `base64 -w 0 keystore/release.keystore` |
| `KEYSTORE_PASSWORD` | store 密码 |
| `KEY_ALIAS` | key 别名 |
| `KEY_PASSWORD` | key 密码 |

> 本仓库当前 keystore：2692 字节 → base64 应为 **3592 个字符**。如果 secret 长度不对，基本就是粘错了。
> 换签名会导致已安装用户无法覆盖升级，请务必备份并长期使用同一份 keystore。

构建产物会按签名结果命名：

- `radio-apk-<run号>-signed`：已签名，可直接安装
- `radio-apk-<run号>-unsigned`：未签名（secret 缺失或错误），产物不可安装，同时会在运行摘要里报错

## 发版流程

1. （可选）改 `app/build.gradle.kts` 里的 `versionName`；tag 构建会自动用 tag 名作为 `versionName`
2. 提交：`git commit -am "release: v1.0.1"` 然后 `git push`
3. 打标签：`git tag v1.0.1 && git push origin v1.0.1`
4. Actions 自动构建签名 APK 并创建 GitHub Release

> `versionCode` 由 CI 自动生成（UTC `yyMMddHH`，例如 `25110615`），保证每次构建都递增、可以直接覆盖安装；
> 本地手动构建仍用 `app/build.gradle.kts` 里的默认值，也可用 `-PversionCode=xxx -PversionName=x.y.z` 覆盖。

## 目录结构

```
app/src/main/java/com/dragon/fm/
├── MainActivity.kt                 入口、桌面快捷方式、更新弹窗
├── data/
│   ├── api/                        WebDAV、电台 API、更新检查
│   ├── local/                      收藏、M3U、备份、预设
│   └── model/                      数据模型
├── player/                         ExoPlayer 封装、媒体会话服务、睡眠定时
└── ui/
    ├── screens/                    首页、设置页、配色
    ├── components/                 通用组件
    └── viewmodel/MainViewModel.kt  全部业务状态
```

## 致谢

- [LX-FM](https://gitee.com/app-finder/LX-FM) — 原始工程与播放内核
- [Transistor](https://github.com/y20k/transistor) — 首页交互设计灵感
- AndroidX Media3 / ExoPlayer、Coil、OkHttp、Kotlinx Serialization
