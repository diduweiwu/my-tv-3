# 修复播放错误界面文字缺失及交互无响应问题

该计划旨在解决播放失败时 `ErrorFragment` 偶发性文字缺失（只显示图标）以及界面无响应的问题。

## 问题分析
1. **文字缺失**：`ErrorFragment.setMsg()` 在调用时仅尝试更新当前可见的 View。如果 Fragment 尚未添加到 Activity（第一次显示时），其 View 尚未创建，导致设置的错误信息丢失。
2. **交互无响应**：
    - 在 `MainActivity` 中，`requestFocus()` 在 `showFragment` 之后立即调用。由于 `FragmentTransaction.commit()` 是异步的，此时 `errorFragment.view` 可能仍为 null。
    - 当 `playerFragment` 被隐藏时，如果焦点没有成功转移到 `errorFragment`，TV 设备可能因失去焦点而无法响应 D-pad 事件。
    - `ErrorFragment` 的根布局虽然设置了 `isFocusable`，但未设置 `isFocusableInTouchMode`，在某些 TV 系统上可能影响焦点获取。

## 拟议变更

### [界面组件]

#### [MODIFY] [ErrorFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ErrorFragment.kt)
- **持久化消息**：增加一个 `msg` 变量存储错误信息。在 `setMsg` 时保存该值，并在 `onViewCreated` 或 `onCreateView` 中应用，确保文字不会丢失。
- **增强焦点获取**：设置 `isFocusableInTouchMode = true`。

#### [MODIFY] [MainActivity.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainActivity.kt)
- **延迟聚焦**：在 `errorFragment` 显示后，使用 `post` 任务来请求焦点，确保此时 View 已经创建并附加。
- **安全检查**：在 `onKey` 中增加更多日志，以便排查按键流转情况。

## 验证计划

### 功能验证
1. **模拟首次报错**：清理应用缓存或重启应用，触发一个播放错误，确认“播放错误”文字正常显示。
2. **焦点验证**：在报错页面，确认遥控器方向键能触发 `MainActivity` 的日志，且能正常换台。
3. **层级验证**：确认在报错页面能正常弹出菜单和设置。

### 鲁棒性验证
- 连续快速切换频道导致报错，确认不会出现黑屏或完全死锁的情况。
