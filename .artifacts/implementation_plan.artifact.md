# 修复 TV 上二维码弹窗尺寸适配与定位问题

## 问题分析

### 现象
- 在 TV 上只显示背景遮罩，二维码弹窗内容未显示。
- 在模拟器和手机上正常。

### 根本原因分析
1.  **窗口定位问题**：目前 `ModalFragment` 在 `onStart()` 中将窗口设置为 `MATCH_PARENT`，但 `modal.xml` 的根布局是 `wrap_content`。在全屏窗口中，`wrap_content` 的布局默认可能位于左上角。在 TV 上，由于 **Overscan（过扫描）** 机制，左上角往往在屏幕显示区域之外，导致看起来像没有内容。
2.  **尺寸计算不一致**：虽然使用了屏幕高度的 80%，但 TV 设备可能存在多种分辨率兼容模式。使用 `MyTVApplication` 中预设的 `shouldHeightPx()` 会更稳定。
3.  **缺少居中设置**：窗口全屏后，未明确要求内容居中显示。

## 方案设计

### 1. 修改布局文件 `modal.xml`
- 将根布局 `LinearLayout` 的 `layout_width` 和 `layout_height` 修改为 `match_parent`。
- 确保 `gravity="center"` 能够将子元素（二维码和文本）居中。

### 2. 优化 `ModalFragment.kt`
- 在 `onStart()` 中显式设置窗口居中。
- 使用 `MyTVApplication` 提供的屏幕高度进行尺寸计算。
- 增加容错逻辑，确保 `size` 不会超过屏幕宽度。

## 待修改文件

### [MODIFY] [modal.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/modal.xml)
- 修改 `layout_width` 和 `layout_height` 为 `match_parent`。

### [MODIFY] [ModalFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ModalFragment.kt)
- 优化 `onStart` 中的窗口配置。
- 优化 `onViewCreated` 中的尺寸计算。

## 验证计划
1. 在 TV 模拟器上验证二维码是否在屏幕正中央。
2. 验证赞赏弹窗（双图）是否也能正确居中显示。
