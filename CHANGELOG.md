# Tempo Mod Changelog

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