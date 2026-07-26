# 自增效版本号实现展示

## ✅ 任务完成

已成功实现打包构建时自动递增版本号的功能。

## 📝 修改内容

### 文件：`app/build.gradle.kts`

#### 1. 添加必要的导入
```kotlin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
```

#### 2. 修改 `getVersionCode()` 函数
**之前**（基于 git tag）：
```kotlin
fun getVersionCode(): Int {
    return try {
        val arr = (getTag().replace(".", " ").replace("-", " ") + " 0").split(" ")
        arr[0].toInt() * 16777216 + arr[1].toInt() * 65536 + arr[2].toInt() * 256 + arr[3].toInt()
    } catch (_: Exception) {
        1
    }
}
```

**之后**（基于时间戳自增）：
```kotlin
fun getVersionCode(): Int {
    // 基准时间：2024-01-01 00:00:00 UTC 的时间戳（毫秒）
    val baseTimeMillis = 1704067200000L
    val currentTimeMillis = System.currentTimeMillis()
    // 计算从基准时间开始的分钟数，确保每次构建自动递增
    return ((currentTimeMillis - baseTimeMillis) / (1000 * 60)).toInt()
}
```

#### 3. 修改 `getVersionName()` 函数
**之前**：
```kotlin
fun getVersionName(): String {
    return getTag().ifEmpty {
        "0.0.0-1"
    }
}
```

**之后**：
```kotlin
fun getVersionName(): String {
    return getTag().ifEmpty {
        // 无 git tag 时使用日期格式
        val sdf = SimpleDateFormat("yyyy.MM.dd-HH", Locale.getDefault())
        sdf.format(Date())
    }
}
```

## 🔍 方案说明

| 项目 | 说明 |
|------|------|
| **versionCode** | 从 2024-01-01 开始计算的分钟数，每次构建自动递增 |
| **versionName** | 优先使用 git tag，无 tag 时使用 `yyyy.MM.dd-HH` 格式日期 |
| **可用年限** | 约 70 年（到 2094 年左右） |
| **最大值** | 不会超过 Android 限制（2,100,000,000） |

## ✅ 验证结果

```bash
./gradlew app:assembleDebug --stacktrace
# 构建成功！
```

## 📋 后续建议

1. **发布版本时**：建议手动创建 git tag（如 `v1.5.0`），这样 `versionName` 会自动使用标签名
2. **version.json**：发布时需要同步更新 `version.json` 文件
3. **本地调试**：无 tag 时，`versionName` 会显示为日期格式（如 `2026.07.26-14`）

## 🎉 总结

现在每次运行构建命令时，`versionCode` 都会自动递增，无需手动修改版本号文件，大大提高了开发效率！
