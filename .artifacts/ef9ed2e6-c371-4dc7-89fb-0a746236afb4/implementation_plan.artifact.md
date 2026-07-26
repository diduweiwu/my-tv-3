# 修复分组宽度自适应算法

## 问题分析
看日志没有报错，但分组组件完全没有宽度。问题出在：
1. `px2PxFont(20f)` 返回的值极小（因为除以了 scale），导致 Paint textSize 只有几像素
2. `paint.measureText()` 测量出的文字宽度几乎为 0
3. 最终计算出的宽度只剩 40dp 边距，看起来就像"挤成一坨"

## 根因
```kotlin
fun px2PxFont(px: Float): Float {
    return (px * ratio / scale).toFloat()  // 错误！除以 scale 导致值极小
}
```

对于 20sp 字体，在 ratio=1.0, scale=2.0 的屏幕上：
- `px2PxFont(20f) = 20 * 1.0 / 2.0 = 10px` ← 这是 Paint 的 textSize，太小了！
- 实际 20sp 应该是 `20 * scale = 40px`

## 修复方案

### [MODIFY] [MenuFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MenuFragment.kt)
- 在 `calculateGroupWidth()` 中：
  1. 使用 `application.sp2Px(20f)` 而不是 `px2PxFont(20f)` 来设置 Paint 的 textSize
  2. 使用 `application.sp2Px(18f)` 测量分组名称
  3. 使用 `application.dp2Px(40)` 计算边距
- 这样 Paint 测量出来的才是真实的屏幕像素宽度

## 验证计划
- 运行 App，打开频道列表
- 观察左侧分组栏宽度是否正常显示
- 确认分组名称完整可见，不再挤压
