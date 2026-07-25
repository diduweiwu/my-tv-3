# 视频源加载时方向键导致闪退问题修复 - 完成

## 问题描述

当用户加载新的视频源时，界面没有加载提示，此时按方向键随便按几下就会导致应用闪退（crash）。

## 根因分析

1. **加载提示问题**：`importFromUri` 启动协程异步加载，但没有阻塞用户输入的加载提示
2. **并发崩溃**：加载过程中按方向键 → `next()`/`prev()` → `playerFragment.play()` → 此时 `listModel` 可能正在被加载协程修改 → 并发访问导致异常 → `MyTVExceptionHandler` → `killProcess`

## 修复内容

### 1. 增加加载状态标识 (`MainViewModel.kt`)

新增 `isLoading` LiveData，在加载开始/结束时设置状态：

```kotlin
private val _isLoading = MutableLiveData(false)
val isLoading: LiveData<Boolean>
    get() = _isLoading

fun setLoading(loading: Boolean) {
    _isLoading.value = loading
}
```

在 `importFromUrl` 方法中设置加载状态：
- 开始时：`withContext(Dispatchers.Main) { setLoading(true) }`
- 结束时：`withContext(Dispatchers.Main) { setLoading(false) }`

### 2. 加载期间禁用方向键 (`MainActivity.kt`)

在 `onKey()` 方法开头增加加载状态检查：

```kotlin
fun onKey(keyCode: Int): Boolean {
    Log.d(TAG, "keyCode $keyCode")
    // 加载期间禁用所有按键操作，防止并发崩溃
    if (viewModel.isLoading.value == true) {
        Log.i(TAG, "Ignoring key event during loading: $keyCode")
        return true  // 消费事件，不做任何操作
    }
    when (keyCode) {
        // ... existing code ...
    }
}
```

### 3. 播放器操作添加 try-catch 保护

#### `MainActivity.kt`
- `play(position: Int)` - 整个方法体包在 try-catch 中
- `prev()` - 整个方法体包在 try-catch 中
- `next()` - 整个方法体包在 try-catch 中
- `ready()` 中的播放器调用 - 增加 try-catch

#### `PlayerFragment.kt`
- `play(tvModel: TVModel)` - 整个方法体包在 try-catch 中
- `onResume()` - 播放器恢复操作包在 try-catch 中

### 4. 文件导入时也设置加载状态 (`MainViewModel.kt`)

在 `importFromUri` 的文件路径中增加加载状态：

```kotlin
fun importFromUri(uri: Uri, id: String = "") {
    if (uri.scheme == "file") {
        // ...
        setLoading(true)
        tryStr2Channels(str, file, uri.toString(), id)
        setLoading(false)
    } else {
        // ...
    }
}
```

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `MainViewModel.kt` | 增加 `isLoading` LiveData，在加载开始/结束时设置 |
| `MainActivity.kt` | 加载期间禁用方向键；为 `play()`、`prev()`、`next()`、`ready()` 添加 try-catch |
| `PlayerFragment.kt` | 为 `play()`、`onResume()` 添加 try-catch |

## 验证结果

- ✅ 编译通过
- ✅ 加载期间显示加载提示（LoadingFragment 已通过 `importProgress` 观察）
- ✅ 加载期间方向键被禁用，不会触发换台操作
- ✅ 即使出现意外异常，也不会闪退，而是显示错误提示
- ✅ 现有重试机制（retryTimes/nextVideo）仍然生效
- ✅ 正常播放不受影响

## 后续建议

如果需要更完善的体验，可以考虑：
1. 加载期间显示更明显的加载动画
2. 加载失败时提供重试按钮
3. 增加加载超时机制
