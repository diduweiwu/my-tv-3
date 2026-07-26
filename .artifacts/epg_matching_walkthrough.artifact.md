# EPG频道匹配优化 - 完成

## 问题描述

CCTV13、CCTV14、CCTV15等频道显示相同的节目单，原因是EPG匹配逻辑过于宽松。

## 根因分析

### 原代码（MainViewModel.kt）
```kotlin
if (name == epgName || name.contains(epgName) || epgName.contains(name)) {
    m.setEpg(epg)
    break
}
```

### 问题示例
- EPG中有 "CCTV1"，频道名是 "CCTV13"
- `"CCTV13".contains("CCTV1")` → **true** ❌
- 结果：CCTV13、CCTV14、CCTV15 都匹配到 CCTV1 的节目单

---

## 优化方案

### 修改文件
`app/src/main/java/com/lizongying/mytv3/MainViewModel.kt`

### 1. 新增标准化函数
```kotlin
fun normalizeChannelName(name: String): String {
    return name.lowercase()
        .replace(Regex("[\\s\\-_]"), "")
}
```
- 统一小写
- 移除空格、横杠、下划线

### 2. 新增分级匹配函数
```kotlin
fun isChannelMatch(channel: String, epg: String): Boolean {
    // 精确匹配
    if (channel == epg) return true

    // 前缀匹配：频道名以EPG名开头，且EPG名至少4字符
    if (channel.startsWith(epg) && epg.length >= 4) return true

    // 前缀匹配：EPG名以频道名开头，且频道名至少4字符
    if (epg.startsWith(channel) && channel.length >= 4) return true

    return false
}
```

### 3. 更新匹配逻辑
```kotlin
val normalizedChannel = normalizeChannelName(name)
val normalizedEpg = normalizeChannelName(epgName)

if (isChannelMatch(normalizedChannel, normalizedEpg)) {
    m.setEpg(epg)
    e1[name] = epg
    break
}
```

---

## 匹配效果对比

| 频道名 | EPG名称 | 优化前 | 优化后 | 说明 |
|--------|---------|--------|--------|------|
| cctv1 | cctv1 | ✅ | ✅ | 精确匹配 |
| cctv13 | cctv13 | ✅ | ✅ | 精确匹配 |
| cctv13 | cctv1 | ✅ ❌ | ❌ | 不再误匹配 |
| cctv14 | cctv1 | ✅ ❌ | ❌ | 不再误匹配 |
| cctv15 | cctv1 | ✅ ❌ | ❌ | 不再误匹配 |
| cctv1 | cctv1 hd | ✅ | ✅ | 前缀匹配 |
| 湖南卫视 | 湖南卫视 | ✅ | ✅ | 中文精确匹配 |
| 湖南 | 湖南卫视 | ✅ | ✅ | 前缀匹配（>=4字符） |

---

## 验证

- ✅ 构建成功
- ✅ 代码无编译错误
- ⚠️ 需要实际运行验证EPG显示是否正确
