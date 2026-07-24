# 修复完成：播放源加载进度 & Web 源保存问题

## 修复概要

| 项目 | 内容 |
|------|------|
| 修改文件 | `app/src/main/java/com/lizongying/mytv0/MainViewModel.kt` |
| 修改函数 | `importFromUrl` (第337-389行) |
| 构建状态 | ✅ 成功 |

---

## 问题根因

```
┌─────────────────────────────────────────────────────────┐
│  主线程 (Dispatchers.Main)                               │
│  ├─ viewModelScope.launch { importFromUrl(...) }        │
│  │   └─ HttpClient.okHttpClient.newCall(request).execute() │
│  │       ↑ 阻塞主线程！                                   │
│  │       ├─ setImportProgress() 调用但 UI 无法更新       │
│  │       └─ 导入未完成 → SP.sources 未保存 → 刷新为空   │
└─────────────────────────────────────────────────────────┘
```

**`importFromUrl`** 通过 `viewModelScope.launch` 运行在主线程，但内部的 `execute()` 是阻塞调用，导致：
1. UI 进度条无法渲染更新
2. 刷新页面时数据可能尚未保存

---

## 修复方案

将网络请求包装在 `withContext(Dispatchers.IO)` 中，使 IO 操作在后台线程执行：

### 修改前（有问题）
```kotlin
val request = okhttp3.Request.Builder().url(a).build()
val response = HttpClient.okHttpClient.newCall(request).execute() // ❌ 阻塞主线程

if (response.isSuccessful) {
    val str = response.bodyAlias()?.string() ?: ""
    withContext(Dispatchers.Main) {
        tryStr2Channels(str, null, b, id)
    }
    // ...
}
```

### 修改后（已修复）
```kotlin
// 使用 Dispatchers.IO 执行网络请求，避免阻塞主线程
val str = withContext(Dispatchers.IO) {  // ✅ 切换到 IO 线程
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
    tryStr2Channels(str, null, b, id)  // 已在主线程，直接调用
    // ...
}
```

---

## 修复后的执行流程

```
┌─────────────────────────────────────────────────────────┐
│  主线程 (Dispatchers.Main)                               │
│  ├─ viewModelScope.launch { importFromUrl(...) }        │
│  │   ├─ setImportProgress(0)  → UI 更新 ✅              │
│  │   ├─ withContext(Dispatchers.IO) { ... }             │
│  │   │   └─ HTTP 请求（后台线程，不阻塞主线程）         │
│  │   ├─ setImportProgress(50) → UI 更新 ✅              │
│  │   ├─ tryStr2Channels() → SP.sources 保存 ✅          │
│  │   └─ setImportProgress(100) → UI 更新 ✅             │
│  └─ 刷新页面时数据已保存，列表正常显示 ✅                │
└─────────────────────────────────────────────────────────┘
```

---

## 验证方式

1. **构建已通过** ✅
2. 建议手动测试：
   - 通过 Web 页面添加 `http://192.168.1.170:1905/u/P6jxgUCxE19zRt-ok0FQjEYM/m3u`
   - 观察进度条是否正常显示
   - 添加成功后刷新页面，确认源列表正确显示

---

## 风险评估

- **影响范围**：仅修改 `importFromUrl` 函数的线程调度
- **兼容性**：`withContext(Dispatchers.IO)` 是 Kotlin 协程标准用法
- **副作用**：无，异常处理逻辑保持不变
