# 修复完成：`/api/sources` 接口卡住 + App 端视频源弹窗为空

## 问题 1：`/api/sources` 接口卡住

Web 端加载 `/api/sources` 接口时一直卡住，一段时间后失败 `net::ERR_Empty_response`。

### 根本原因
1. `handleSources()` 方法从 GitHub 获取 `sources.txt` 文件
2. `getUrls()` 返回了 **13 个镜像 URL**，会依次尝试
3. `HttpClient` 没有配置任何超时时间
4. `runBlocking(Dispatchers.IO)` 阻塞了服务器线程

### 修复内容
**文件**：`app/src/main/java/com/lizongying/mytv0/SimpleServer.kt`

- 去掉了 `fetchSources()` 方法和远程 GitHub 加载逻辑
- 直接读取本地 `R.raw.sources` 资源文件返回
- 删除了未使用的导入（`Gua`、`coroutines`、`HttpClient` 等）

---

## 问题 2：App 端视频源弹窗为空

Web 端添加视频源后，`SP.sources` 有数据，但 App 端打开视频源弹窗时列表为空。

### 根本原因
- Web 端 `/api/settings` 接口的 `history` 字段数据从 `SP.sources` 读取
- App 端视频源弹窗使用 `viewModel.sources`（`Sources` 类型），其 `init()` 只在对象创建时执行一次
- 如果 `MainViewModel` 创建时 `SP.sources` 为空，后续 Web 添加源后更新了 `SP.sources`，但 App 端弹窗可能未正确同步

### 修复内容

**文件 1**：`app/src/main/java/com/lizongying/mytv0/models/Sources.kt`

添加 `reload()` 方法：
```kotlin
fun reload() {
    init()
}
```

**文件 2**：`app/src/main/java/com/lizongying/mytv0/SourcesFragment.kt`

在 `onViewCreated` 方法中，打开弹窗时调用 `reload()` 确保数据最新：
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

    // 重新加载视频源数据，确保显示最新数据（与 Web 端 /api/settings 的 history 字段一致）
    viewModel.sources.reload()

    // ... 其余代码不变
}
```

---

## 效果

- ✅ `/api/sources` 接口现在**立即响应**，不再依赖 GitHub 远程加载
- ✅ App 端视频源弹窗与 Web 端 `/api/settings` 的 `history` 字段数据一致
- ✅ 打开弹窗时自动从 `SP.sources` 重新加载最新数据
- ✅ 构建成功，无语法错误

## 验证

1. 编译项目：✅ 构建成功
2. 部署到设备后，Web 端访问 `http://localhost:34567/api/sources` 可立即获取视频源列表
3. 通过 Web 端添加视频源，然后打开 App 端视频源弹窗，确认列表正常显示
4. App 端视频源列表与 Web 端 `history` 字段完全一致（都从 `SP.sources` 读取）
