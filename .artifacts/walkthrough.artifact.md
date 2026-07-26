# 频道 Logo 解析优化与 UI 布局调整说明

本次更新优化了频道 Logo 的匹配逻辑，精简了错误日志，并调整了切换频道时底部信息悬浮窗的布局。

## 主要变更

### 1. 频道 Logo 名称匹配优化
- **智能切分**：在根据频道名称匹配 Logo 文件（如备用 Gitee 库）时，系统现在会自动按空格切分名称并仅取第一部分。
    - *示例*：`"CCTV-16 奥运 4K"` 将自动匹配为 `"CCTV-16.png"`。
- **涉及文件**：
    - [MainViewModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/MainViewModel.kt)
    - [ImageHelper.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/ImageHelper.kt)

### 2. 日志精简
- **去除堆栈**：在图片下载或加载失败时，仅打印错误简述（如 `HTTP 404` 或 `Timeout`），不再输出冗长的异常堆栈信息（Stack Trace），使 Logcat 日志更加整洁。

### 3. 悬浮信息窗 UI 调整
- **序号右移**：将原本位于最左侧的频道序号（Channel Number）移到了最右侧。
- **视觉优化**：同步调换了左侧和右侧容器的圆角背景（`rounded_dark_left` 与 `rounded_dark_right`），确保视觉上的圆滑过渡依然自然。
- **涉及文件**：
    - [info.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml)

## 验证结果

- **编译状态**：`./gradlew app:assembleDebug` 已成功通过。
- **逻辑验证**：
    - `substringBefore(" ")` 逻辑已正确应用。
    - `Log.e` 调用的 Throwable 参数已移除。
- **布局验证**：`info.xml` 中的子元素顺序已更新，且背景资源匹配正确。

> [!TIP]
> 如果部分频道的 Logo 仍然不显示，请确认 Logo 库中是否存在以“切分后第一部分名称”命名的 PNG 文件。
