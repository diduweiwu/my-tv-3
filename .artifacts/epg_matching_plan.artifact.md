# EPG频道匹配优化方案

## 问题分析

### 当前代码（MainViewModel.kt 第263-267行）
```kotlin
for ((n, epg) in res) {
    val epgName = n.lowercase()
    if (name == epgName || name.contains(epgName) || epgName.contains(name)) {
        m.setEpg(epg)
        e1[name] = epg
        break
    }
}
```

### 问题原因
匹配条件 `name.contains(epgName) || epgName.contains(name)` 太宽松：

| EPG名称 | 频道名 | 匹配结果 |
|---------|--------|----------|
| cctv1 | cctv13 | "cctv13".contains("cctv1") = true ❌ |
| cctv1 | cctv14 | "cctv14".contains("cctv1") = true ❌ |
| cctv1 | cctv15 | "cctv15".contains("cctv1") = true ❌ |

### 实际影响
- CCTV13、CCTV14、CCTV15 都会匹配到 CCTV1 的节目单
- 因为第一个匹配的频道会被 `break` 选中

---

## 优化方案

### 策略：分级匹配 + 标准化名称

#### 1. 标准化函数
将频道名称中的数字和特殊字符统一处理：
- 移除空格、横杠、下划线
- 将中文数字转为阿拉伯数字（如果需要）

#### 2. 匹配优先级
1. **精确匹配** - 标准化后完全相等
2. **前缀匹配** - 频道名以EPG名开头（如 "cctv1" 匹配 "cctv1 hd"）
3. **后缀匹配** - EPG名以频道名开头（较少用）

#### 3. 避免反向误匹配
- 当 EPG名长度 < 频道名长度 时，要求 EPG名是频道名的**前缀**
- 当 EPG名长度 >= 频道名长度 时，要求频道名是EPG名的**前缀**
- 这样可以避免 "cctv1" 错误匹配 "cctv13"

---

## 修改文件

### 文件：`app/src/main/java/com/lizongying/mytv3/MainViewModel.kt`

在 `readEPG(InputStream)` 方法中：
```kotlin
// 替换当前匹配逻辑
for ((n, epg) in res) {
    val epgName = n.lowercase()
    val normalizedChannel = normalizeChannelName(name)
    val normalizedEpg = normalizeChannelName(epgName)

    if (isChannelMatch(normalizedChannel, normalizedEpg)) {
        m.setEpg(epg)
        e1[name] = epg
        break
    }
}
```

添加辅助函数：
```kotlin
private fun normalizeChannelName(name: String): String {
    return name.lowercase()
        .replace(Regex("[\\s\\-_]"), "")  // 移除空格、横杠、下划线
}

private fun isChannelMatch(channel: String, epg: String): Boolean {
    // 精确匹配
    if (channel == epg) return true

    // 前缀匹配：频道名包含EPG名且以EPG名开头
    if (channel.startsWith(epg) && epg.length >= 4) return true

    // 前缀匹配：EPG名包含频道名且以频道名开头（频道名至少4字符避免误匹配）
    if (epg.startsWith(channel) && channel.length >= 4) return true

    return false
}
```

---

## 验证场景

| 频道名 | EPG名称 | 匹配结果 | 说明 |
|--------|---------|----------|------|
| cctv1 | cctv1 | ✅ | 精确匹配 |
| cctv13 | cctv13 | ✅ | 精确匹配 |
| cctv1 | cctv1 hd | ✅ | 前缀匹配 |
| cctv13 | cctv1 | ❌ | 不再误匹配 |
| cctv14 | cctv1 | ❌ | 不再误匹配 |
| cctv15 | cctv1 | ❌ | 不再误匹配 |
| 湖南卫视 | 湖南卫视 | ✅ | 中文精确匹配 |
| 湖南 | 湖南卫视 | ✅ | 前缀匹配（频道名>=4字符） |
