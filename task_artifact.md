# ANR修复任务追踪

## 任务列表

- [x] 修改 `importFromUrl` 方法 - 调用新的异步方法 `tryStr2ChannelsAsync`
- [x] 添加 `tryStr2ChannelsAsync` 方法 - 后台线程耗时计算 + 主线程更新UI
- [x] 添加 `parseChannelsFromStr` 方法 - 在后台线程执行 Gua 解码和解析
- [x] 拆分 `str2Channels` 为 `parseChannels` + `applyChannelsToState`
- [x] 修改 `importFromUri` 方法 - file scheme 路径调用异步方法
- [x] 修改 `SimpleServer.handleImportText` - 使用异步方法
- [x] 构建验证 - 确保编译通过
