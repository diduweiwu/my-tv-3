# 修复计划：播放源加载进度 & Web 源保存问题

## 问题总结

| 问题 | 现象 | 根本原因 |
|------|------|----------|
| 1 | 加载播放源一直"没进度" | `importFromUrl` 在主线程执行阻塞网络请求，UI 无法更新 |
| 2 | Web 页面添加源后刷新列表为空 | 同上，导入未完成时刷新导致数据未保存 |

## 修复方案

### 修改文件
- `app/src/main/java/com/lizongying/mytv0/MainViewModel.kt`

### 具体改动

#### 1. 修复 `importFromUrl` 函数（第337-389行）

**当前问题：**
```kotlin
private suspend fun importFromUrl(url: String, id: String = "") {
    // ... 运行在 Dispatchers.Main ...
    val response = HttpClient.okHttpClient.newCall(request).execute() // 阻塞主线程！
    // ...
}
```

**修复方案：**
将网络请求切换到 `Dispatchers.IO` 执行，确保主线程不被阻塞：

```kotlin
private suspend fun importFromUrl(url: String, id: String = "") {
    val urls = getUrls(url).map { Pair(it, url) }

    var err = 0
    var shouldBreak = false
    for ((index, pair) in urls.withIndex()) {
        val a = pair.first
        val b = pair.second
        Log.i(TAG, "request $a")
        setImportProgress(index * 50 / urls.size)
        try {
            val str = withContext(Dispatchers.IO) {
                val request = okhttp3.Request.Builder().url(a).build()
                val response = HttpClient.okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    response.bodyAlias()?.string() ?: ""
                } else {
                    Log.e(TAG, "Request status ${response.codeAlias()}")
                    err = R.string.channel_status_error
                    ""
                }
            }

            if (err == 0 && str.isNotEmpty()) {
                tryStr2Channels(str, null, b, id)
                err = 0
                shouldBreak = true
            }
        } catch (e: JsonSyntaxException) {
            // ... 异常处理保持不变
        }
        // ...
    }
    // ...
}
```

### 关键修改点

1. **使用 `withContext(Dispatchers.IO)` 包装网络请求**
   - 确保网络请求在 IO 线程执行
   - 主线程不会被阻塞，UI 进度更新可以正常显示

2. **移除不必要的 `withContext(Dispatchers.Main)`**
   - 因为 `importFromUrl` 已经在主线程调用
   - `tryStr2Channels` 直接调用即可

## 验证方式

1. 构建并部署应用
2. 通过 Web 页面添加一个网络播放源（如 `http://192.168.1.170:1905/u/P6jxgUCxE19zRt-ok0FQjEYM/m3u`）
3. 观察加载进度是否正常显示（"正在加載播放源...X%"）
4. 添加成功后刷新 Web 页面，确认源列表正确显示
