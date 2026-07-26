# 执行计划：禁用版本检查 + 移除六十四卦编码链接

## 需求概述
1. 禁用版本检查功能（不删除代码，仅禁用调用）
2. 移除 Web 页面上的"六十四卦编码"链接提示

---

## 变更清单

### 1. 禁用版本检查调用

**文件**: `app/src/main/java/com/lizongying/mytv0/SettingFragment.kt`

**变更点 1** (约第 361 行):
```java
// 原代码
} else {
    updateManager.checkAndUpdate()
}

// 修改为：注释掉调用，保留代码
} else {
    // updateManager.checkAndUpdate()  // 已禁用
    Log.i(TAG, "版本检查已禁用")
}
```

**变更点 2** (约第 408 行):
```java
// 原代码
if (allPermissionsGranted) {
    updateManager.checkAndUpdate()
} else {
    Log.w(TAG, "ask permissions failed")
}

// 修改为：注释掉调用，保留代码
if (allPermissionsGranted) {
    // updateManager.checkAndUpdate()  // 已禁用
    Log.i(TAG, "版本检查已禁用")
} else {
    Log.w(TAG, "ask permissions failed")
}
```

---

### 2. 移除六十四卦编码链接

**文件**: `app/src/main/res/raw/index.html`

**变更位置** (约第 166-168 行):
```html
<!-- 原代码 -->
<p style="font-size: 0.8rem; color: #90A4AE; margin-top: 0.5rem;">
    视频源文本转换：<a target="_blank" href="https://lizongying.github.io/js-gua64/">六十四卦编码</a>
</p>

// 修改为：删除或注释掉整行
```

---

## 验证方式
1. 构建项目确认无编译错误
2. 进入设置页面，确认点击"检查更新"不再触发网络请求
3. 打开 Web 页面，确认"六十四卦编码"链接已消失

---

## 风险评估
- **低风险**：版本检查只是注释掉调用入口，UpdateManager 类完整保留
- **低风险**：六十四卦编码链接只是提示文字，不影响 App 内部的解码功能
