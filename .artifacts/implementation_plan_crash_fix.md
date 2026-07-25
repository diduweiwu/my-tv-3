# 视频源加载时方向键导致闪退问题修复方案

## 问题分析

### 用户反馈
- 加载新的视频源时，界面上**没有加载提示**
- 加载过程中按方向键随便按几下就会**闪退（crash）**

### 根因定位

**场景还原：**
1. 用户在 `SourcesFragment` 选择视频源
2. `onItemClicked()` 调用 `dismiss()` + `viewModel.importFromUri(uri)`
3. `importFromUri` 启动协程异步加载：`viewModelScope.launch { importFromUrl(...) }`
4. **此时主界面可见，但没有阻塞用户输入的加载提示**
5. 用户按方向键 → `channelUp()`/`channelDown()` → `next()`/`prev()`
6. `next()`/`prev()` 调用 `tvModel?.setReady()` → 触发 observer → `playerFragment.play(it)`
7. `play()` 中 `tvModel.getMediaItem()` 访问 `tv.uris`，可能与加载协程的 `str2Channels` 修改冲突
8. 或者 TVModel 处于不一致状态，`Uri.parse()` 等操作抛出未捕获异常 → `MyTVExceptionHandler` → `killProcess`

**关键代码路径：**
```
方向键 → onKey() → channelUp()/channelDown()
  → next()/prev()
  → viewModel.groupModel.getNext()/getPrev()
  → setReady()
  → observer: playerFragment.play(it)
  → play() 中访问 tvModel.tv.uris (可能正在被修改)
  → 异常 → killProcess
```

### 修复策略

1. **加载期间显示加载提示** — 确保用户看到正在加载
2. **加载期间禁用方向键换台** — 阻塞用户输入，防止并发操作
3. **为播放器关键路径添加 try-catch 保护** — 防御性编程，避免闪退

## 修改点

### 1. `MainViewModel.kt` — 增加加载状态标识

```kotlin
// 新增：是否正在加载视频源
private val _isLoading = MutableLiveData(false)
val isLoading: LiveData<Boolean>
    get() = _isLoading

fun setLoading(loading: Boolean) {
    _isLoading.value = loading
}
```

在 `importFromUrl` 开始和结束时设置加载状态：

```kotlin
private suspend fun importFromUrl(url: String, id: String = "") {
    withContext(Dispatchers.Main) { setLoading(true) }
    try {
        // ... existing code ...
    } finally {
        withContext(Dispatchers.Main) { setLoading(false) }
    }
}
```

在 `tryStr2Channels` 中也设置加载状态：

```kotlin
fun tryStr2Channels(str: String, file: File?, url: String, id: String = "") {
    setLoading(true)
    try {
        // ... existing code ...
    } finally {
        setLoading(false)
    }
}
```

### 2. `MainActivity.kt` — 加载期间禁用方向键

在 `onKey()` 方法开头增加加载状态检查：

```kotlin
private fun onKey(keyCode: Int): Boolean {
    // 加载期间禁用所有换台操作，防止并发崩溃
    if (viewModel.isLoading.value == true) {
        Log.i(TAG, "Ignoring key event during loading: $keyCode")
        return true  // 消费事件，不做任何操作
    }
    
    when (keyCode) {
        // ... existing code ...
    }
}
```

### 3. `PlayerFragment.kt` — 为 play() 添加 try-catch

```kotlin
@OptIn(UnstableApi::class)
fun play(tvModel: TVModel) {
    try {
        this.tvModel = tvModel
        player?.stop()
        player?.clearMediaItems()
        
        binding.playerView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        binding.playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        binding.playerView.requestLayout()

        if (isAdded) {
            val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            viewModel.setVideoFormatInfo("")
        }
        player?.run {
            val videoUrl = tvModel.getVideoUrl() ?: return
            if (videoUrl.isEmpty()) {
                tvModel.setErrInfo(R.string.play_error.getString())
                return
            }

            while (true) {
                val last = tvModel.isLastVideo()
                val mediaItem = tvModel.getMediaItem()
                if (mediaItem == null) {
                    if (last) {
                        tvModel.setErrInfo(R.string.play_error.getString())
                        break
                    }
                    tvModel.nextVideo()
                    continue
                }
                val mediaSource = tvModel.getMediaSource()
                if (mediaSource != null) {
                    setMediaSource(mediaSource)
                } else {
                    setMediaItem(mediaItem)
                }
                prepare()
                break
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "play failed", e)
        tvModel.setErrInfo(R.string.play_error.getString())
    }
}
```

### 4. `PlayerFragment.kt` — 为 onResume() 添加 try-catch

```kotlin
override fun onResume() {
    super.onResume()
    try {
        if (player?.isPlaying == false) {
            player?.prepare()
            player?.play()
        }
    } catch (e: Exception) {
        Log.e(TAG, "resume play failed", e)
        tvModel?.setErrInfo(R.string.play_error.getString())
    }
}
```

### 5. `MainActivity.kt` — 为 ready() 中的播放器调用添加保护

在 `ready()` 方法的 `it.ready.observe` 中：

```kotlin
it.ready.observe(this) { ready ->
    if (ready != null) {
        try {
            Log.i(TAG, "${it.tv.title} 嘗試播放")
            hideFragment(errorFragment)
            showFragment(playerFragment, PlayerFragment.TAG)
            playerFragment.play(it)
            infoFragment.show(it)
            if (SP.channelNum) {
                channelFragment.show(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
            showLoadError(getString(R.string.play_error))
        }
    }
}
```

### 6. `MainActivity.kt` — 为 play() 方法添加 try-catch

```kotlin
fun play(position: Int): Boolean {
    return try {
        if (position > -1 && position < viewModel.groupModel.getAllList()!!.size()) {
            // ... existing code ...
            true
        } else {
            R.string.channel_not_exist.showToast()
            false
        }
    } catch (e: Exception) {
        Log.e(TAG, "play failed", e)
        showLoadError(getString(R.string.play_error))
        false
    }
}
```

### 7. `MainActivity.kt` — 为 prev()/next() 添加 try-catch

```kotlin
fun prev() {
    try {
        // ... existing code ...
    } catch (e: Exception) {
        Log.e(TAG, "prev failed", e)
        showLoadError(getString(R.string.play_error))
    }
}

fun next() {
    try {
        // ... existing code ...
    } catch (e: Exception) {
        Log.e(TAG, "next failed", e)
        showLoadError(getString(R.string.play_error))
    }
}
```

## 预期效果

- ✅ 加载期间显示加载提示，用户知道正在加载
- ✅ 加载期间方向键被禁用，不会触发换台操作
- ✅ 即使出现意外异常，也不会闪退，而是显示错误提示
- ✅ 现有重试机制（retryTimes/nextVideo）仍然生效
- ✅ 正常播放不受影响

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| `MainViewModel.kt` | 增加 `isLoading` LiveData，在加载开始/结束时设置 |
| `MainActivity.kt` | 加载期间禁用方向键；为 `play()`、`prev()`、`next()`、`ready()` 添加 try-catch |
| `PlayerFragment.kt` | 为 `play()`、`onResume()` 添加 try-catch |

## 风险评估

- **低风险**：修改主要是增加保护性代码，不改变正常流程
- **兼容性**：不影响正常播放逻辑
- **用户体验**：加载期间无法换台，但能看到加载提示，体验更好
