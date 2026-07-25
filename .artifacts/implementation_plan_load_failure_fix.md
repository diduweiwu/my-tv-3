# 视频源加载失败恢复方案

## 问题分析

当用户在 `SourcesFragment` 选择视频源并确认后：
1. `dismiss()` 关闭弹窗
2. `viewModel.importFromUri(uri)` 开始异步加载
3. 加载过程中，`LoadingFragment` 显示进度
4. **失败时的问题**：
   - `tryStr2Channels` / `importFromUrl` 只显示 Toast，然后调用 `setImportProgress(null)` 隐藏加载
   - 没有任何错误屏幕提示用户
   - 用户无法重试，也无法返回之前的播放状态
   - 应用看起来卡住了

## 修复方案

### 目标
- 加载失败时显示错误屏幕（复用现有 `ErrorFragment`）
- 提供明确的操作选项：重试 / 返回 / 换源
- 用户可以继续操作 app 而不是卡住

### 修改点

#### 1. `MainActivity.kt` - 新增加载失败显示方法

添加 `showLoadError(message: String)` 方法，在加载失败时显示错误屏幕：

```kotlin
fun showLoadError(message: String) {
    hideFragment(loadingFragment)
    errorFragment.setMsg(message)
    showFragment(errorFragment, ErrorFragment.TAG)
    errorFragment.startCountdown {
        // 倒计时结束后返回菜单，让用户可以选择其他操作
        showFragment(menuFragment, MenuFragment.TAG)
    }
}
```

#### 2. `MainActivity.kt` - 监听加载失败

在 `watch()` 方法中观察加载进度，检测加载失败：

```kotlin
// 跟踪是否正在加载
var isLoading = false

viewModel.importProgress.observe(this) { progress ->
    if (progress != null) {
        isLoading = true
        val text = if (progress < 100) {
            "正在加載播放源...${progress}%"
        } else {
            "正在解析頻道數據..."
        }
        loadingFragment.showProgress(text)
        showFragment(loadingFragment, LoadingFragment.TAG)
    } else {
        isLoading = false
        loadingFragment.hideProgress()
        hideFragment(loadingFragment)
    }
}
```

#### 3. `MainViewModel.kt` - 新增加载失败事件

添加 `loadError` LiveData，当加载失败时通知 Activity：

```kotlin
private val _loadError = MutableLiveData<String?>()
val loadError: LiveData<String?>
    get() = _loadError

fun setLoadError(error: String?) {
    _loadError.value = error
}
```

#### 4. `MainViewModel.kt` - 在 importFromUrl 失败时触发事件

修改 `importFromUrl`，在失败时调用 `setLoadError`：

```kotlin
if (err != 0) {
    err.showToast()
    setLoadError(err.getString())  // 新增：通知 Activity 显示错误屏幕
}
setImportProgress(null)
```

#### 5. `MainViewModel.kt` - 在 tryStr2Channels 失败时触发事件

```kotlin
} else {
    R.string.channel_import_error.showToast()
    Log.w(TAG, "channel import error")
    setLoadError(getString(R.string.channel_import_error))  // 新增
    setImportProgress(null)
}
```

#### 6. `MainActivity.kt` - 观察加载失败事件

在 `watch()` 方法中：

```kotlin
viewModel.loadError.observe(this) { error ->
    if (error != null) {
        showLoadError(error)
        viewModel.setLoadError(null)  // 消费后重置
    }
}
```

#### 7. `SourcesFragment.kt` - 加载失败时不关闭弹窗（可选改进）

或者更好的方式：在 SourcesFragment 监听加载状态，加载失败时保持弹窗或重新打开。

**更简单的方案**：直接在 SourcesFragment 中监听加载状态，因为 SourcesFragment 是一个 DialogFragment，我们可以：

```kotlin
// 在 SourcesFragment 中
viewModel.importProgress.observe(this) { progress ->
    if (progress == null && /* 失败条件 */) {
        // 重新显示弹窗或保持显示
    }
}
```

但考虑到 DialogFragment 已被 dismiss，最简洁的方式是通过 Activity 显示错误屏幕。

## 关键文件修改

1. `MainActivity.kt` - 新增 `showLoadError()` 方法 + 观察 `loadError` 事件
2. `MainViewModel.kt` - 新增 `loadError` LiveData + 失败时触发
3. `ErrorFragment.kt` - 可能微调：支持"重试"操作（可选）

## 用户体验

1. 用户选择视频源 → LoadingFragment 显示进度
2. 加载失败 → ErrorFragment 显示错误信息（如"视频源加载失败"）
3. 5秒倒计时 → 自动返回菜单
4. 用户可以按 BACK/ENTER 提前返回
5. 用户可以选择其他视频源或恢复默认配置

## 实施步骤

1. 修改 `MainViewModel.kt` 添加 `loadError` LiveData
2. 修改 `MainViewModel.kt` 在失败时触发 `loadError`
3. 修改 `MainActivity.kt` 添加 `showLoadError()` 方法
4. 修改 `MainActivity.kt` 在 `watch()` 中观察 `loadError`
5. 编译验证
6. 部署测试
