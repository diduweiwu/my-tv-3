# 播放源切换时自动隐藏弹窗

## 问题
切换播放源或加载播放源后，左边（菜单/节目单）或右边（设置）的弹窗一直显示，不会自动隐藏。

## 弹窗当前隐藏机制
- **menuFragment**：`menuActive()` 重置10秒计时器
- **settingFragment**：`settingActive()` 重置3分钟计时器
- **programFragment**：需手动返回或 `menuActive()`

## 修改方案

### 文件1: `MainActivity.kt`
添加 `hideAllPopups()` 方法，隐藏所有侧边弹窗：
```kotlin
fun hideAllPopups() {
    hideFragment(menuFragment)
    hideFragment(programFragment)
    hideFragment(settingFragment)
    handler.removeCallbacks(hideMenu)
    handler.removeCallbacks(hideSetting)
    showTimeFragment()
}
```

在 `channelsOk` 观察器中，加载完成后调用：
```kotlin
viewModel.channelsOk.observe(this) { it ->
    if (it) {
        // ... 现有逻辑 ...
        hideAllPopups()  // 新增：隐藏所有弹窗
        hideFragment(loadingFragment)
    }
}
```

### 文件2: `SourcesFragment.kt`
在 `onItemClicked` 中，点击源后立即隐藏弹窗：
```kotlin
override fun onItemClicked(position: Int, tag: String) {
    viewModel.sources.getSource(position)?.let {
        val uri = Uri.parse(it.uri)
        viewModel.viewModelScope.launch {
            viewModel.importFromUri(uri, it.id ?: "")
        }
    }

    // 立即隐藏弹窗
    (activity as? MainActivity)?.hideAllPopups()
    dismiss()
}
```

### 文件3: `SettingFragment.kt`（可选）
在远程配置加载源时，也隐藏弹窗：
- `confirmConfig` 按钮点击后
- 远程配置通过 SimpleServer 触发加载后

## 验证
1. 打开菜单 → 切换源 → 菜单自动隐藏
2. 打开设置 → 切换源 → 设置自动隐藏
3. 打开节目单 → 切换源 → 节目单自动隐藏
