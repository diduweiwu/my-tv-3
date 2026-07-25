# 视频源加载时方向键导致闪退问题修复

## 任务清单

### 阶段1：MainViewModel 添加加载状态
- [x] 添加 `isLoading` LiveData
- [x] 在 `importFromUrl` 开始/结束时设置
- [x] 在 `tryStr2Channels` 中设置（通过 `importFromUri` 的文件路径）

### 阶段2：MainActivity 加载期间禁用方向键 + try-catch 保护
- [x] `onKey()` 开头增加加载状态检查
- [x] `play()` 方法添加 try-catch
- [x] `prev()` 方法添加 try-catch
- [x] `next()` 方法添加 try-catch
- [x] `ready()` 中的播放器调用添加 try-catch

### 阶段3：PlayerFragment 添加 try-catch 保护
- [x] `play()` 方法添加 try-catch
- [x] `onResume()` 方法添加 try-catch

### 阶段4：验证
- [x] 编译通过

## 状态：完成
