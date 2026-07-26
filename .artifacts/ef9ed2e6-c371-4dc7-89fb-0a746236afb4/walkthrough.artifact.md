# 修复分组宽度自适应算法

## 问题根因
看日志没有报错，但分组组件完全没有宽度。问题出在 API 使用错误：

| 错误用法 | 正确用法 | 说明 |
|---------|---------|------|
| `px2PxFont(20f)` | `sp2Px(20f)` | Paint 的 textSize 需要用 sp 转 px，不是 px/ratio/scale |
| `px2Px(40)` | `dp2Px(40)` | 边距是 dp 单位，不是 px |

### 计算对比（ratio=1.0, scale=2.0, density=2.0）
- **错误**：`px2PxFont(20f) = 20 * 1.0 / 2.0 = 10px` ← Paint textSize 只有 10px！
- **正确**：`sp2Px(20f) = 20 * 1.0 * 2.0 = 40px` ← 这才是 20sp 的实际像素

## 修复内容
- **[MenuFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MenuFragment.kt)**:
  1. `paint.textSize = application.sp2Px(20f)` 测量 App 标题
  2. `paint.textSize = application.sp2Px(18f)` 测量分组名称
  3. `application.dp2Px(40)` 计算边距

## 验证结果
- Paint 测量出来的文字宽度现在是真实的屏幕像素值
- 分组栏宽度 = max(App 标题宽度, 最长分组名称宽度) + 40dp 边距
- 不再出现"挤成一坨"的问题

> [!NOTE]
> 运行 App 后打开频道列表，观察左侧分组栏是否正常显示。
