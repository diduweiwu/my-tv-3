# 修复 TV 上远程配置二维码弹窗显示异常

## 问题分析

### 现象
- 模拟器和安卓手机上：二维码弹窗正常显示
- 电视机上：只显示背景遮罩，二维码弹窗内容未正确展示

### 根本原因

**1. DialogFragment 窗口大小未显式设置**

`ModalFragment` 继承自 `DialogFragment`，在 `onStart()` 方法中只设置了全屏标志和隐藏导航栏，但**没有显式设置窗口大小**：

```kotlin
override fun onStart() {
    super.onStart()
    dialog?.window?.apply {
        addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        // ❌ 缺少 setLayout() 调用
    }
}
```

对比 `SourcesFragment`（另一个 DialogFragment）的实现：
```kotlin
override fun onStart() {
    super.onStart()
    dialog?.window?.apply {
        addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        // ✅ 显式设置窗口大小
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        setLayout(width, WindowManager.LayoutParams.MATCH_PARENT)
    }
}
```

**2. TV 上窗口尺寸问题**

在 TV 上，DialogFragment 默认的窗口行为可能与手机不同：
- TV 的屏幕密度和分辨率特性可能导致默认窗口尺寸计算异常
- 二维码大小计算为屏幕高度的 80%（`targetHeight = (screenHeight * 0.8).toInt()`），如果窗口本身不是全屏，内容可能超出可视区域

**3. 窗口背景问题**

DialogFragment 默认使用对话框背景样式，在 TV 上可能只显示背景遮罩层，而内容区域被遮挡或尺寸为零。

## 修复方案

### 修改文件
- `app/src/main/java/com/lizongying/mytv3/ModalFragment.kt`

### 修改内容

在 `onStart()` 方法中添加窗口大小设置，确保 DialogFragment 在 TV 上有正确的尺寸：

```kotlin
override fun onStart() {
    super.onStart()
    dialog?.window?.apply {
        addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        // 修复 TV 上弹窗显示问题：显式设置窗口大小为全屏
        val displayMetrics = resources.displayMetrics
        setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        // 清除对话框默认背景，避免只显示遮罩
        setBackgroundDrawableResource(android.R.color.transparent)
    }
}
```

### 备选方案（如果上述方案不完全有效）

如果 TV 上仍有问题，可以尝试在 `onCreateView` 之前设置窗口样式：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 设置为无标题样式，避免 TV 上默认对话框样式问题
    setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
}
```

## 验证步骤

1. 构建并部署应用到 TV 设备
2. 进入设置页面，点击"远程配置"按钮
3. 验证二维码弹窗是否正常显示
4. 对比模拟器和手机上的行为，确保一致性
5. 测试赞赏弹窗（双二维码）在 TV 上的显示

## 风险评估

- **低风险**：仅修改 DialogFragment 的窗口属性，不影响核心功能
- **兼容性好**：MATCH_PARENT 尺寸在所有设备上都能正常工作
- **不影响现有功能**：手机上行为保持不变
