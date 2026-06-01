# Tempo Mod Bug List

## 已修复 Bug

### 1. 桌面歌词开关点击闪退 ✅ 3.9.0.4
**描述**: 点击开启桌面歌词时，应用闪退而不是弹出权限请求通知

**复现步骤**:
1. 进入设置 → 界面
2. 点击"桌面歌词"开关

**预期行为**: 应该弹出系统权限请求，提示用户授予悬浮窗权限

**实际行为**: 应用直接闪退

**根因**: `SettingsFragment.requestOverlayPermission()` 用了 `setClassName("com.android.settings", "com.android.settings.ManageApplicationsActivity")` 直接锁定"应用管理" Activity，HyperOS 上这个 Activity 不存在或未导出，触发 `ActivityNotFoundException` 闪退

**修复**: 改为标准 `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` intent + `package:` URI，加 `try/catch` 兜底 `ACTION_APPLICATION_DETAILS_SETTINGS`

**修复文件**: `app/src/main/java/com/cappielloantonio/tempo/ui/fragment/SettingsFragment.java`

---

## CHANGELOG

### 3.9.0.4
- 新增系统播放器歌词（锁屏/通知栏/灵动岛显示当前歌词行）
- 修复桌面歌词开关点击闪退

### 3.9.0.3
- 通知栏歌词：默认关闭
- 桌面歌词：默认关闭
- 修复切歌歌词更新问题
- 修复开关逻辑独立控制问题