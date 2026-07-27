# 修复播放错误后换台不显示频道详情的问题

## 问题分析
在之前的更改中，为了确保错误提示和菜单不被遮挡，我们在 `MainActivity.kt` 的 `showFragment` 方法中增加了 `bringToFront()` 调用。
然而，当播放出错显示 `ErrorFragment` 后，用户进行换台操作时，`MainActivity` 会重新调用 `showFragment(playerFragment, ...)`。
这导致基础播放图层 `playerFragment` 被带到了最顶层，从而遮挡了本应在其上方的 `infoFragment`（显示频道详情的悬浮窗）和 `channelFragment`（显示频道数字的悬浮窗）。

## 拟议变更

### [核心逻辑]

#### [MODIFY] [MainActivity.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainActivity.kt)
- 修改 `showFragment` 方法，在调用 `bringToFront()` 时排除 `playerFragment`。
- 这样可以确保 `playerFragment` 始终作为最底层的背景图层，而 `infoFragment` 和 `channelFragment` 等叠加层能够正常显示在其上方。

## 验证计划

### 手动验证
1. 制造一个播放错误（如断开网络），确认错误页面显示。
2. 恢复网络或切换到一个有效的频道。
3. 确认换台后，屏幕下方能正常显示频道详情悬浮窗（`InfoFragment`）。
4. 确认在正常播放状态下，按下数字键能正常显示频道数字悬浮窗（`ChannelFragment`）。
5. 确认菜单和设置界面依然能正常显示在最顶层，不会被背景遮挡。
