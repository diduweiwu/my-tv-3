# ANR修复完成报告

## 问题回顾
用户选择视频源确认后，整个界面冻结，按键无反应，最终闪退。日志显示：
```
Reason: Input dispatching timed out (Waited 5002ms for FocusEvent(hasFocus=true))
```

## 根本原因

### 核心问题
`str2Channels()` 包含两类操作混杂在一起：
- **耗时计算**：Gua.decode、JSON.parse、正则匹配、遍历构建TV对象 → 应在后台线程
- **LiveData赋值**：`groupModel.addTVListModel()`、`groupModel.setChange()` → 必须在主线程

之前的错误修复将整个方法移到后台线程，导致LiveData在非主线程赋值引发崩溃。

### 修复策略
将 `str2Channels()` 拆分为纯计算和状态更新两部分，通过协程在线程间切换。

## 修改内容

### 1. 新增 `tryStr2ChannelsAsync()` — 异步入口
```kotlin
suspend fun tryStr2ChannelsAsync(str: String, file: File?, url: String, id: String = "") {
    // 1. 后台线程(Dispatchers.Default)：解析频道（Gua解码+JSON解析）
    val list = withContext(Dispatchers.Default) { parseChannelsFromStr(str) }
    
    if (list != null) {
        // 2. 后台线程(Dispatchers.IO)：文件写入
        withContext(Dispatchers.IO) { cacheFile?.let { it.writeText(str) } }
        
        // 3. 主线程(Dispatchers.Main)：更新LiveData和UI
        withContext(Dispatchers.Main) {
            applyChannelsToState(list, str)
            _channelsOk.value = true
            R.string.channel_import_success.showToast()
        }
    }
}
```

### 2. 新增 `parseChannelsFromStr()` — 后台线程解析
```kotlin
private suspend fun parseChannelsFromStr(str: String): List<TV>? {
    // Gua解码（耗时操作，在Dispatchers.Default执行）
    val decoded = withContext(Dispatchers.Default) {
        val g = Gua()
        if (g.verify(str)) g.decode(str) else str
    }
    return parseChannels(decoded)  // 纯计算解析
}
```

### 3. 新增 `applyChannelsToState()` — 主线程更新
```kotlin
private fun applyChannelsToState(list: List<TV>, str: String) {
    groupModel.initTVGroup()
    // 创建 TVModel、TVListModel
    groupModel.addTVListModel(listTVModel)  // LiveData赋值
    groupModel.setChange()  // LiveData赋值
}
```

### 4. 修改 `str2Channels()` — 同步版本用于初始化
```kotlin
private fun str2Channels(str: String): Boolean {
    val list = parseChannels(string)  // 解析
    applyChannelsToState(list, string)  // 更新状态
    return true
}
```

### 5. 修改调用方
| 文件 | 修改 |
|------|------|
| `importFromUrl()` | 调用 `tryStr2ChannelsAsync()` 替代直接调用 |
| `importFromUri()` | file scheme 调用异步版本 |
| `SimpleServer.handleImportText()` | 使用 `runBlocking` 调用异步版本 |
| `MainActivity.kt` | isp.observe 中用 Thread 执行异步 |

## 线程模型对比

| 操作 | 修复前 | 修复后 |
|------|--------|--------|
| 网络请求 | IO线程 ✓ | IO线程 ✓ |
| Gua解码 | **主线程** ❌ | Default线程 ✓ |
| JSON解析 | **主线程** ❌ | Default线程 ✓ |
| 正则匹配 | **主线程** ❌ | Default线程 ✓ |
| 文件写入 | **主线程** ❌ | IO线程 ✓ |
| LiveData赋值 | 主线程 ✓ | 主线程 ✓ |

## 构建验证
```
gradle assembleDebug ✅ 构建成功
```

## 修改文件清单
- `MainViewModel.kt` — 核心修复，新增3个方法，修改3个方法
- `SimpleServer.kt` — 使用异步方法
- `MainActivity.kt` — 使用后台线程
