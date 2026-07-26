# Implementation Plan

## Request 1: 将UA字段移到"远程配置"区域

### 当前状态
- `ua_display` TextView 位于 `setting.xml` 的 App Header 区域（第60-67行）
- 显示当前UA值，但位置不够直观

### 目标状态
- 将 `ua_display` 移到 "Remote Config" 区域，紧跟在"远程配置"和"切换源"按钮下方
- 让用户在远程配置区域就能看到UA信息

### 修改文件
1. **`app/src/main/res/layout/setting.xml`**
   - 将 `ua_display` TextView 从 App Header 区域（第60-67行）移到 Remote Config 区域（第71行之后）
   - 调整样式使其与按钮区域协调

2. **`app/src/main/java/com/lizongying/mytv3/SettingFragment.kt`**
   - 无需修改，因为 `binding.uaDisplay.text = SP.ua ?: "Linux-6"` 这行代码保持不变

---

## Request 2: 每次启动app时加载EPG

### 当前状态
- `updateEPG()` 在 `MainActivity.ready()` 方法中通过 `channelsOk.observe` 触发
- 这意味着EPG加载需要等待：
  1. ViewModel.init() 完成（加载频道、缓存EPG）
  2. Fragment 准备就绪
  3. channelsOk 变为 true
- 如果 `configAutoLoad` 为 false，EPG只在此时加载

### 目标状态
- 在 `MainViewModel.init()` 中频道加载完成后立即启动EPG加载
- 这样可以在Fragment准备的同时并行加载EPG，加快EPG显示速度

### 修改文件
1. **`app/src/main/java/com/lizongying/mytv3/MainViewModel.kt`**
   - 在 `init()` 方法中，`str2Channels(cacheChannels)` 之后
   - 添加 `viewModelScope.launch { updateEPG() }` 调用
   - 这样EPG加载会在频道解析完成后立即开始，无需等待Fragment就绪

---

## Verification

1. **UA字段位置验证**
   - 运行应用，打开设置页面
   - 确认UA显示在"远程配置"按钮下方
   - 确认UA值正确显示

2. **EPG加载验证**
   - 启动应用，观察EPG加载日志
   - 确认EPG在启动时即开始加载（查看logcat中的 "EPG $a success" 日志时间）
   - 切换频道时EPG信息应立即显示
