# 悬浮详情弹窗节目单增强计划

用户希望在频道切换时的悬浮详情弹窗中展示两个节目（当前和下一个），并包含每个节目的时间段信息，格式参考左键节目列表。

## 用户审查确认

> [!IMPORTANT]
> 1. **展示内容**：展示两行节目。第一行是当前节目，第二行是下一个节目。
> 2. **信息完善**：每行都包含时间段（HH:mm-HH:mm）和节目名称，例如：`12:00-13:00  正在播放：精彩节目`。
> 3. **布局调整**：弹窗高度将从 `80dp` 增加到 `110dp`。
> 4. **视觉风格**：时间与名称之间保持适当间距，字体大小与原 `desc` 保持一致。

## 待办任务

### 1. 调整 `info.xml` 布局
- **[MODIFY] [info.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml)**
    - 将 `info` 容器的高度从 `80dp` 增加到 `110dp`。
    - 在 `desc` 之后添加新的 `AppCompatTextView` (id: `desc_next`) 用于展示下一节目。
    - 适当调整 `main` 容器的内边距，使两行节目文字在卡片中垂直分布更均衡。

### 2. 更新 `InfoFragment.kt` 逻辑
- **[MODIFY] [InfoFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/InfoFragment.kt)**
    - 在 `onCreateView` 中，为 `desc_next` 设置动态缩放的文字大小。
    - 修改 `show(tvModel: TVModel)` 方法中的 EPG 处理逻辑：
        - 获取当前时间戳。
        - 从 `tvModel.epg.value` 中提取数据。
        - 查找当前节目：`beginTime <= now < endTime`。
        - 查找紧随其后的节目。
        - 使用 `Utils.formatTimeRange(epg)` (或手动拼接 `Utils.getDateFormat`) 格式化时间段。
        - 拼接最终字符串：`HH:mm-HH:mm  正在播放：[标题]` 和 `HH:mm-HH:mm  稍后播放：[标题]`。
        - 更新 `desc` 和 `desc_next`。如果没有数据，显示合理占位符或隐藏第二行。

## 验证计划

### 自动验证
- 检查代码编译是否通过。

### 手动验证
- 运行应用并切换频道。
- 确认弹窗高度增加，且垂直方向展示了两行节目。
- 确认每行节目都有 `HH:mm-HH:mm` 格式的时间段前缀。
- 确认切换至没有 EPG 数据的频道时，显示“精彩节目”且不会出现空行导致的布局畸形。
