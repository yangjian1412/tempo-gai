# Tempo Mod Changelog

## Version 3.9.0.5 (开发中)

**基于版本**: Tempo 3.9.0.4 (本 Mod 上一版)

**开发者**: 六分仪 (Liuyi)

**概述**:
3.9.0.5 修复设置项显示顺序 bug,新增桌面歌词行数设置、桌面歌词锁屏显示开关、通知栏歌词锁屏显示开关,并在此过程中修复了行数/字号/颜色等设置一直默认值的根因 bug。

**Bug 修复**:

- **设置界面显示顺序错误**:桌面歌词的字号/颜色/背景透明度选项原本显示在系统播放器歌词选项下方,现已调整到桌面歌词开关正下方,所有桌面歌词子选项聚拢成一组
- **桌面歌词行数设置不起作用** (`getDesktopLyricsLineCount`):根本原因是使用 `getInt()` 读取 `ListPreference` 持久化的值,但 `ListPreference` 实际存的是 String,导致 `getInt()` 抛 `ClassCastException` 后被 try-catch 吞掉,始终返回默认值。改为 `getString()` + `toIntOrNull()` 解析。
- **桌面歌词锁屏显示不生效**:`FLAG_SHOW_WHEN_LOCKED` 设置逻辑不够鲁棒,flag 变化时原代码会销毁并重建整个 view。改为 flag 变化时只调用 `updateViewLayout()` 更新已有 view 的 flags,并注册 `ACTION_SCREEN_ON` / `ACTION_USER_PRESENT` 广播接收器,确保锁屏→解锁后窗口布局被重新应用。
- **字号/颜色设置一直默认值**:与行数同一根因,顺便一并修复。

**新增功能**:

- **桌面歌词行数设置**(设置 → 歌词 → 桌面歌词 → 歌词行数)
  - 可选 1 行 / 2 行 / 3 行 / 4 行(默认 2 行)
  - 1 行:仅显示当前行
  - 2 行:当前行 + 下一行
  - 3 行:上一行 + 当前行 + 下一行
  - 4 行:上一行 + 当前行 + 下一行 + 下两行

- **桌面歌词锁屏显示开关**:已删除
  - 原因:在小米/HyperOS 上,`FLAG_SHOW_WHEN_LOCKED` 需要用户去系统设置单独授权,代码层无法绕开,实测不稳定
  - 替代:需要锁屏看歌词时,使用"通知栏歌词 + 锁屏显示"

- **通知栏歌词锁屏显示开关**(设置 → 歌词 → 通知栏歌词 → 锁屏显示,默认开)
  - 开启:锁屏后通知栏仍显示完整歌词内容(`VISIBILITY_PUBLIC`)
  - 关闭:锁屏后通知栏隐藏歌词内容(`VISIBILITY_SECRET`)

- **歌词设置独立成组**:在设置界面把"通知栏歌词 / 桌面歌词 / 系统播放器歌词"及相关子项从原来的"界面"分类中抽出,新建独立的"歌词"分类。

- **歌词主开关与子选项联动**:关闭"通知栏歌词"主开关时,"锁屏显示"子选项自动 disable;关闭"桌面歌词"主开关时,5 个桌面歌词子选项(字号/颜色/背景透明度/行数/锁屏显示)自动 disable。

**修改文件**:
- `app/src/main/java/.../util/Preferences.kt` — 改用 `getString()` + 解析;新增 3 个偏好
- `app/src/main/java/.../service/DesktopLyricsOverlay.kt` — 支持 4 行 + 6 个字号
- `app/src/main/java/.../service/NotificationHelper.kt` — `setVisibility()` 改为读取偏好
- `app/src/main/java/.../ui/fragment/SettingsFragment.java` — 新增 `actionLyricsNotificationDependency` / `actionDesktopLyricsDependency`
- `app/src/tempo/.../service/MediaService.kt` — `DesktopLyricsOverlay.show()` 调用传 4 行
- `app/src/main/res/values/arrays.xml` — `desktop_lyrics_line_count` 加第 4 项;`desktop_lyrics_font_size` 扩到 6 个;`desktop_lyrics_color` 扩到 11 个
- `app/src/main/res/values/strings.xml` — 加 5 个字符串 + `settings_title_lyrics = "歌词"`
- `app/src/main/res/layout/desktop_lyrics_overlay.xml` — 新增 `desktop_lyrics_next2` TextView
- `app/src/main/res/xml/global_preferences.xml` — 抽出独立的"歌词"分类
- `app/build.gradle` — versionCode 28, versionName 3.9.0.5

## Version 3.9.0.4

**基于版本**: Tempo 3.9.0.4 (本 Mod 上一版)

**开发者**: 六分仪 (Liuyi)

**概述**:
3.9.0.5 修复了设置项显示顺序 bug,并新增桌面歌词行数设置、桌面歌词锁屏显示开关、通知栏歌词锁屏显示开关。

**Bug 修复**:
- 设置界面显示顺序错误:桌面歌词的字号/颜色/背景透明度选项原本显示在系统播放器歌词选项下方,现已调整到桌面歌词开关正下方,所有桌面歌词子选项聚拢成一组

**新增功能**:

- **桌面歌词行数设置**(设置 → 歌词 → 桌面歌词 → 歌词行数)
  - 可选 1 行 / 2 行 / 3 行(默认 2 行)
  - 1 行:仅显示当前行
  - 2 行:当前行 + 下一行
  - 3 行:上一行 + 当前行 + 下一行(更适合跟唱)

- **桌面歌词锁屏显示开关**(设置 → 歌词 → 桌面歌词 → 锁屏显示,默认开)
  - 开启:锁屏后桌面歌词仍悬浮显示
  - 关闭:锁屏后桌面歌词隐藏(使用 WindowManager `FLAG_SHOW_WHEN_LOCKED` 控制)

- **通知栏歌词锁屏显示开关**(设置 → 歌词 → 通知栏歌词 → 锁屏显示,默认开)
  - 开启:锁屏后通知栏仍显示完整歌词内容(`VISIBILITY_PUBLIC`)
  - 关闭:锁屏后通知栏隐藏歌词内容(`VISIBILITY_SECRET`)

**修改文件**:
- `app/src/main/res/xml/global_preferences.xml` — 重新排列顺序,新增 3 个偏好项
- `app/src/main/res/values/strings.xml` — 新增 6 个字符串(行数 + 2 个锁屏)
- `app/src/main/res/values/arrays.xml` — 新增 `desktop_lyrics_line_count` 数组
- `app/src/main/res/layout/desktop_lyrics_overlay.xml` — 新增 `desktop_lyrics_prev` TextView
- `app/src/main/java/.../util/Preferences.kt` — 新增 3 个偏好常量 + getter/setter
- `app/src/main/java/.../service/DesktopLyricsOverlay.kt` — `show()` / `updateLyrics()` 改 4 行入参,新增锁屏 flag 处理
- `app/src/main/java/.../service/NotificationHelper.kt` — `setVisibility()` 改为读取偏好
- `app/src/tempo/.../service/MediaService.kt` — `DesktopLyricsOverlay.show()` 调用传 4 行
- `app/build.gradle` — versionCode 28, versionName 3.9.0.5

## Version 3.9.0.4

**基于版本**: Tempo 3.9.0 (原作者: CappielloAntonio, versionCode 26)

**开发者**: 六分仪 (Liuyi)

**概述**:
3.9.0.4 引入"系统播放器歌词"功能:在小米 HyperOS 锁屏播放器、通知栏播放器、灵动岛播放器上同步显示当前歌词行。本功能通过用户在自己调试设备上抓 logcat 反推实现原理,使用标准 MediaSession 公开 API,不调用任何 HyperOS 私有 API。

**新增功能**:
- 系统播放器歌词:设置 → 界面 → 系统播放器歌词(默认关闭)
- 开启后,锁屏/通知栏/灵动岛播放器第一行显示当前歌词行,第二行显示 `原artist - 原歌名`
- 关闭后,锁屏/通知栏/灵动岛播放器立即恢复原样(第一行歌名,第二行 artist)

**实现原理(关键发现,由用户在 HyperOS 设备上抓 logcat 确认)**:
- 小米 MiPlay SDK 的 `Cir_Miplay_MetaInfoManager` 监听所有活跃 MediaSession
- SDK 把 `MediaMetadata.title` 映射到内部 `MediaMetaData.mTitle`,作为灵动岛/锁屏的显示文本
- **椒盐音乐就是用这个机制**:它把当前歌词行 setMediaMetadata 到 `title` 字段(实测 logcat 显示 `MediaPlayerWrapper.onMetadataChanged` 中 title 每 1-2 秒变化一次,内容为歌词行)
- Tempo 模仿椒盐方案:在 100ms 周期循环里,歌词行变化时调用 `player.replaceMediaItem` 注入新 MediaMetadata

**MediaMetadata 注入方式**:
- `title` 字段 = 当前歌词行(让锁屏第一行显示歌词)
- `artist` 字段 = `原artist - 原歌名`(让锁屏第二行仍能看到歌曲信息)
- `extras["original_title"]` = 备份原歌名(仅第一次注入时保存,防止被注入值覆盖)
- `extras["original_artist"]` = 备份原 artist(同上)
- 关掉开关时,从 extras 恢复 title 和 artist 到原值

**修改内容**:
- `MediaService` 新增 `injectLyricsIntoMediaSession()` / `restoreOriginalMetadata()` 方法
- `updateLyricsNotification()` 在系统播放器歌词开关开启且有歌词时调用注入,否则调用恢复
- `onMediaItemTransition` 跳过 `MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED`,避免 replaceMediaItem 触发的元数据更新被误判为切歌
- 新增偏好 `system_player_lyrics`(`Preferences.SYSTEM_PLAYER_LYRICS`)
- 新增字符串 `settings_system_player_lyrics_title` / `settings_system_player_lyrics_summary`

**已知限制**:
- 注入通过 `player.replaceMediaItem` 实现,理论上可能引起极短暂的 source 重新准备(实际使用未观察到明显卡顿)
- 歌词行变化时(每 1-5 秒一次)才触发注入,不在每 100ms 周期里反复注入

**适用版本**: HyperOS / MIUI 13+(基于实测设备 HyperOS 3 / Android 16),其他系统未测试

**Bug 修复**:
- 修复桌面歌词开关闪退 bug:`SettingsFragment.requestOverlayPermission()` 原先使用 `android.settings.action.APPLICATION_SETTINGS` + `com.android.settings/com.android.settings.ManageApplicationsActivity` 跳转悬浮窗权限页,在 HyperOS 上找不到目标 Activity 触发 `ActivityNotFoundException` 导致闪退。改为使用标准 `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` intent,并增加 `ACTION_APPLICATION_DETAILS_SETTINGS` 作为兜底,带 `try/catch` 防止极端情况再闪退

---

## Version 3.9.0.3

**基于版本**: Tempo 3.9.0 (原作者: CappielloAntonio, versionCode 26)

**开发者**: 六分仪 (Liuyi)

**概述**:
Tempo Mod 是基于原作者 CappielloAntonio 的 Tempo 3.9.0 版本进行二次开发的音乐播放器，主要增加了通知栏歌词和桌面歌词功能。

**新增功能**:
- 通知栏歌词显示：播放歌曲时，通知栏显示当前歌词（4行视图）
- 歌词通知开关：设置中可开启/关闭歌词通知
- 桌面歌词显示：悬浮窗口贴在屏幕底部，显示当前行和下一行预览
- 桌面歌词位置可拖动调整，位置自动保存
- 桌面歌词颜色选择：白、黄、粉、蓝、绿、紫 6种颜色
- 桌面歌词长按打开应用

**修改内容**:
- 优化歌词通知视图，白天/夜间模式自动适配
- 有歌词时隐藏标题和艺术家信息
- 删除歌词通知的展开状态，简化视图
- 切歌时自动重新获取歌词并更新通知栏和桌面歌词
- 桌面歌词字体大小设置
- 桌面歌词背景透明度设置
- 修复切歌后歌词不更新的问题
- 修复开关逻辑独立控制问题

**设置项**:
- 设置 → 界面 → 通知栏歌词：默认关闭
- 设置 → 界面 → 桌面歌词：默认关闭
- 设置 → 界面 → 字体大小：小/中/大
- 设置 → 界面 → 歌词颜色：白/黄/粉/蓝/绿/紫
- 设置 → 界面 → 背景透明度：0-100%

**技术实现**:
- 使用 WindowManager + TYPE_APPLICATION_OVERLAY 悬浮窗
- 需要 SYSTEM_ALERT_WINDOW 权限

---

## Bug List

### 桌面歌词开关点击闪退
- 点击开启桌面歌词时，应用闪退而不是弹出权限请求通知
- 需要权限: SYSTEM_ALERT_WINDOW
- 状态: 待修复