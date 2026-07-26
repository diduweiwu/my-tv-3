# 频道 Logo 名称解析优化、日志精简与 UI 调整计划

## 目标说明
1. **名称切分**：在匹配 Logo 文件时，将频道名称按空格切分，仅取第一部分（例如 `"CCTV-16 奥运 4K"` -> `"CCTV-16"`）。
2. **日志精简**：在加载图片出错时，仅打印错误简述，不再打印完整的异常堆栈信息，保持日志整洁。
3. **UI 调整**：切换频道时显示的悬浮信息窗（InfoFragment），将频道序号从最左边移到最右边。

## Proposed Changes

### 1. 频道 Logo 名称处理与日志精简

#### [MODIFY] [MainViewModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/MainViewModel.kt)
- 在 `preloadLogo` 方法中，处理频道名称：使用 `substringBefore(" ")` 获取第一部分作为匹配 Key。
- 修改错误捕获：`Log.e` 只打印消息字符串，不传入 Throwable 对象。

#### [MODIFY] [ImageHelper.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/ImageHelper.kt)
- `preloadImage` 和 `loadImage`：对 `key`（频道名）应用 `substringBefore(" ")`。
- 全局所有 `Log.e` 捕获处，精简日志输出，不打印堆栈。

### 2. 悬浮窗 UI 调整

#### [MODIFY] [info.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml)
- 调整 `LinearLayout` (id: `info`) 内部子元素的顺序。
- 将 `channel_num` 移至 `main` 容器之后（最右侧）。
- **样式适配**：
    - 将原本在左侧的 `channel_num` 的背景从 `rounded_dark_left` 改为 `rounded_dark_right`。
    - 将原本在右侧的 `main` 容器的背景从 `rounded_dark_right` 改为 `rounded_dark_left`。

---

## Verification Plan

### 手动验证
1. **名称切分**：找一个带空格的频道名，确认日志中下载的文件名是简短版。
2. **日志精简**：人为制造网络超时或无效 URL，确认 Logcat 中不再出现长段堆栈。
3. **UI 位置**：换台时观察底部信息窗，频道序号是否已显示在右侧。
