<p align="center">
  <img alt="Tempo" title="Tempo" src="mockup/svg/horizontal_logo.svg" width="250">
</p>

## nas音乐重度用户，navidrome重度使用者，一直在用tempo，但是对于歌词显示功能缺失一直很遗憾。直到aicoding出现。
## 完全0编程基础，全程opencode自然语言对话，成功增加了三个歌词显示功能。
## 之前，我从未想过自己还有机会自己编程，然后还能上传，以至于修改过程中的前三个版本都覆盖掉了。
## 是的，时代变了，感慨完了，以下内容均为ai生成了，其实，我也不知道他描述的功能对不对。

# Tempo Mod 3.9.0.4

> ⚠️ **本项目是基于 [CappielloAntonio/tempo](https://github.com/CappielloAntonio/tempo) 3.9.0 的修改版本（Mod）**
>
> 原项目以 [GPL-3.0](LICENSE) 协议开源，本 Mod 同样以 GPL-3.0 协议发布。
> 详细新增功能、修改说明见下方。

---

## 简介

原版 Tempo 是一款开源的 Subsonic 音乐客户端（[原项目地址](https://github.com/CappielloAntonio/tempo)）。

本 Mod 在原版基础上**新增了三类歌词相关功能**，并修复了一个桌面歌词相关的崩溃 Bug：

1. **桌面歌词悬浮窗** — 在任意界面（不只是播放器内）显示可拖动歌词
2. **通知栏歌词** — 系统通知中心显示4行歌词
3. **系统播放器歌词** — 通过 `MediaSession.title` 注入，让小米/三星等系统的"灵动岛"、"锁屏播放卡片"显示歌词

---

## ✨ 新增功能

### 1. 桌面歌词悬浮窗

- **位置**：设置 → 歌词 → 桌面歌词
- **行为**：开启后请求"显示在其它应用上层"权限，授予后会在屏幕上叠加一个可拖动的歌词窗
- **特性**：
  - 跟随播放进度高亮当前行
  - 可拖动到任意位置
  - 屏幕旋转时位置自动重新计算
  - 退出应用/暂停播放时自动隐藏

### 2. 通知栏歌词

- **位置**：设置 → 歌词 → 通知栏歌词
- **行为**：在系统的媒体通知卡片下方追加通知栏，显示4行歌词
- **特性**：
  - 大/小通知模板都支持
  - 跟随播放进度刷新（约 1 秒一次）
  - 暗色模式自动适配（`layout-night/`）

### 3. 系统播放器歌词（灵动岛/锁屏）

- **位置**：设置 → 歌词 → 系统播放器歌词
- **行为**：通过 Android `MediaSession` 的 `title` 字段周期注入当前歌词行
- **支持的设备**：
  - ✅ 小米 HyperOS（锁屏播放卡片 + 灵动岛）
  - ✅ 三星 One UI（锁屏 + 通知卡片标题）
  - ✅ 其它标准 MediaSession 消费者
- **特性**：
  - 开启时：`title` = 当前歌词行，`artist` = "原 artist - 原 title"
  - 关闭时：从 `extras["original_title"]` / `extras["original_artist"]` 恢复原值
  - 拦截 `onMediaItemTransition(PLAYLIST_CHANGED)`，避免歌词模块被反复触发

---

## 🔧 技术细节

| 改动 | 文件 | 说明 |
|---|---|---|
| 桌面歌词核心 | `app/src/main/java/com/cappielloantonio/tempo/service/DesktopLyricsOverlay.kt` | 新增文件，`WindowManager` 悬浮窗 |
| 桌面歌词布局 | `app/src/main/res/layout/desktop_lyrics_overlay.xml` | 新增文件 |
| 通知栏歌词 | `app/src/main/java/com/cappielloantonio/tempo/service/NotificationHelper.kt` | 新增/修改文件，`RemoteViews` 注入歌词行 |
| 通知栏布局 | `app/src/main/res/layout/notification_small.xml`、`notification_large.xml`、`layout-night/notification_small.xml` | 新增文件 |
| 系统播放器歌词 | `app/src/tempo/java/com/cappielloantonio/tempo/service/MediaService.kt` | 修改 `updateLyricsNotification()`，新增 `injectLyricsIntoMediaSession()` / `restoreOriginalMetadata()` |
| 设置项 | `app/src/main/res/xml/global_preferences.xml` | 新增 SwitchPreferences |
| 中文化 | `app/src/main/res/values/strings.xml` | 翻译歌词相关设置说明 |

### 实现原理（系统播放器歌词）

```kotlin
// 周期执行（约 1 秒）
val currentLyricLine = lyrics[currentPosition]
player.replaceMediaItem(
    currentMediaItem.mediaId,
    currentMediaItem.buildUpon()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(currentLyricLine)        // 注入歌词行
                .setArtist("${originalArtist} - ${originalTitle}")  // 备份原信息
                .setExtras(Extras().apply {
                    putString("original_title", originalTitle)      // 用于恢复
                    putString("original_artist", originalArtist)
                })
                .build()
        )
        .build()
)
```

小米 HyperOS 的 `Cir_Miplay_MetaInfoManager`（MiPlay SDK）会自动监听所有活跃 `MediaSession`，把 `title` 字段映射为灵动岛显示文本——这就是为什么"改 title"就能让系统播放器显示歌词。

---

## 📦 安装

### 下载

前往 [Releases](https://github.com/yangjian1412/tempo-gai/releases) 页面下载 `tempo-mod-3.9.0.4-debug.apk`。

### 系统要求

- Android 5.0+（与原版一致）
- 桌面歌词需要：Android 6.0+（悬浮窗权限从 6.0 开始需要用户授权）
- 系统播放器歌词需要：设备有 MediaSession 消费者（小米/三星/标准 Android 锁屏均可）

### 安装步骤

1. 下载 `tempo-mod-3.9.0.4-debug.apk`
2. 手机上开启"未知来源应用"权限
3. 点击 APK 安装
4. 首次启动会提示授予存储/通知权限
5. **桌面歌词**功能需要在设置里单独开启并授予"显示在其它应用上层"权限
6. 完整体验歌词功能需要配置 Subsonic 服务器并启用"歌词显示"

---
## 📜 协议

本项目以 **GNU General Public License v3.0** 协议发布，与上游 [CappielloAntonio/tempo](https://github.com/CappielloAntonio/tempo) 一致。

详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- **原作者**：[Antonio Cappiello](https://github.com/CappielloAntonio) — 创建并维护了 Tempo 这个优秀的 Subsonic 客户端
- **参考实现**：[椒盐音乐（Salt Player）](https://github.com/Moriafly/SaltPlayerSource) 的 `MediaPlayerWrapper.onMetadataChanged` 给我提供了"通过 title 注入歌词到系统 MediaSession"的思路
- **MiPlay SDK**：小米 HyperOS 灵动岛/锁屏歌词显示依赖 `Cir_Miplay_MetaInfoManager` 服务

---

## 💬 反馈

- 提 Issue：https://github.com/yangjian1412/tempo-gai/issues
- 原始 Tempo 项目 Issue：https://github.com/CappielloAntonio/tempo/issues

**注意**：原版 Tempo 的 Bug 请到原作者仓库反馈；本 Mod 特有的问题（桌面歌词、通知栏歌词、系统播放器歌词相关）请到本仓库反馈。

---

## 📊 版本历史

| 版本 | 日期 | 说明 |
|---|---|---|
| 3.9.0.4 | 2026-05-30 | 首次发布：桌面歌词 + 通知栏歌词 + 系统播放器歌词 + 桌面歌词闪退 Bug 修复 |

