# 修复远程配置二维码和WebView空白问题

## 问题分析

### 问题1：远程配置的二维码出不来了

**根本原因**：
- `SettingFragment.kt` 第39行：`private var server = "http://${PortUtil.lan()}:$PORT"`
- `PortUtil.lan()` 在某些Android设备上可能返回 `null`（当获取本地IP失败时）
- 如果 `lan()` 返回 `null`，URL 变成 `"http://null:34567"`，导致二维码内容无效
- 二维码生成失败或显示异常

### 问题2：安卓上点击二维码区域跳转的页面是空白的

**根本原因**：
- `MainActivity.kt` 的 `showWebViewPopup` 方法中，WebView 缺少必要的客户端配置
- 没有设置 `WebViewClient` 和 `WebChromeClient`
- WebView 可能无法正确处理页面加载、JS交互和错误处理
- PopupWindow 的焦点设置可能影响 WebView 的正常行为

## 修复方案

### 修复1：处理 PortUtil.lan() 返回 null 的情况

**文件**：`app/src/main/java/com/lizongying/mytv3/SettingFragment.kt`

**修改内容**：
- 在 `server` 变量初始化时检查 `PortUtil.lan()` 是否为 null
- 如果为 null，显示错误提示或使用默认值
- 添加日志记录便于调试

### 修复2：增强 WebView 配置

**文件**：`app/src/main/java/com/lizongying/mytv3/MainActivity.kt`

**修改内容**：
- 为 WebView 添加 `WebViewClient` 处理页面加载事件
- 为 WebView 添加 `WebChromeClient` 处理JS对话框、进度等
- 启用 DOM storage 和 other necessary settings
- 添加错误处理和日志
- 确保 PopupWindow 正确显示 WebView

## 详细修改

### SettingFragment.kt 修改

```kotlin
// 修改 server 变量的初始化
private var server = run {
    val ip = PortUtil.lan()
    if (ip != null) {
        "http://$ip:$PORT"
    } else {
        Log.e(TAG, "无法获取本地IP地址")
        // 显示错误提示
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), "无法获取本地IP地址，请检查网络连接", Toast.LENGTH_LONG).show()
        }
        "http://127.0.0.1:$PORT" // 使用本地回环地址作为fallback
    }
}
```

### MainActivity.kt 修改

```kotlin
fun showWebViewPopup(url: String) {
    val binding = SettingsWebBinding.inflate(layoutInflater)

    val webView = binding.web
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        loadWithOverviewMode = true
        useWideViewPort = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }

    webView.isFocusableInTouchMode = true
    webView.isFocusable = true

    // 添加 WebViewClient 处理页面加载
    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d(TAG, "WebView loading: $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "WebView loaded: $url")
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            Log.e(TAG, "WebView error: ${error?.description}")
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return false // 让 WebView 处理 URL 加载
        }
    }

    // 添加 WebChromeClient
    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d(TAG, "WebView console: ${consoleMessage?.message()}")
            return true
        }
    }

    webView.loadUrl(url)

    val popupWindow = PopupWindow(
        binding.root,
        RelativeLayout.LayoutParams.MATCH_PARENT,
        RelativeLayout.LayoutParams.MATCH_PARENT
    )

    popupWindow.inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
    popupWindow.isFocusable = true
    popupWindow.isTouchable = true
    popupWindow.isClippingEnabled = false
    popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    popupWindow.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)

    webView.requestFocus()

    binding.close.setOnClickListener {
        popupWindow.dismiss()
    }
}
```

## 验证步骤

1. 构建并运行应用
2. 在安卓设备上测试远程配置功能
3. 检查二维码是否正常显示
4. 点击二维码区域，检查 WebView 页面是否正常加载
5. 对比模拟器行为，确认问题已解决
