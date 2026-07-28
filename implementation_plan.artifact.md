# Implementation Plan

## 需求1：切换分组后的聚焦定位逻辑

### 修改文件
- `app/src/main/java/com/lizongying/mytv3/MenuFragment.kt`

### 修改内容
在 `onItemClicked(position: Int)` 方法中，更新频道列表后添加聚焦逻辑：

```kotlin
override fun onItemClicked(position: Int) {
    if (!this::viewModel.isInitialized) {
        Log.e(TAG, "viewModel is not initialized")
        return
    }

    viewModel.groupModel.getTVListModel(position)?.let { listModel ->
        if (listModel.size() > 0) {
            groupAdapter.activePosition = position
            groupAdapter.notifyDataSetChanged()

            viewModel.groupModel.setPosition(position)
            SP.positionGroup = position

            (binding.list.adapter as ListAdapter).update(listModel)

            // 分组点击后，将焦点移到频道列表
            groupAdapter.focusable(false)
            listAdapter.focusable(true)

            // 判断是否为当前频道所属分组
            val currentPlayingGroup = viewModel.groupModel.positionPlayingValue
            if (position == currentPlayingGroup) {
                // 是当前频道所属分组，聚焦到当前频道
                listAdapter.toPosition(listModel.positionPlayingValue)
            } else {
                // 不是当前频道所属分组，聚焦到第一个
                listAdapter.toPosition(0)
            }
        }
    }
}
```

---

## 需求2：去掉左侧频道分组和弹窗列表组件的自动隐藏逻辑

### 修改文件
- `app/src/main/java/com/lizongying/mytv3/MainActivity.kt`
- `app/src/main/java/com/lizongying/mytv3/SourcesFragment.kt`

### 修改内容

#### 1. MainActivity.kt
- `menuActive()` 方法：移除自动隐藏逻辑，仅保留方法体（或清空）
- 删除 `hideMenu` Runnable
- 删除 `delayHideMenu` 常量

```kotlin
fun menuActive() {
    // 已移除自动隐藏逻辑
}

// 删除以下代码：
// private val delayHideMenu = 10 * 1000L
// private val hideMenu = Runnable { ... }
```

#### 2. SourcesFragment.kt
- 移除 `onViewCreated` 中的 `handler.postDelayed(hideFragment, delayHideFragment)`
- 移除 `onKey` 中的 `handler.removeCallbacks(hideFragment)` 和 `handler.postDelayed(hideFragment, delayHideFragment)`
- 删除 `delayHideFragment` 常量
- 保留 `hideFragment` 变量（可能其他地方有引用）或一并删除

```kotlin
// 删除以下代码：
// private val delayHideFragment = 10000L
// handler.postDelayed(hideFragment, delayHideFragment)
```

---

## 验证点
1. 点击不同分组后，频道列表是否正确聚焦到第一个或当前频道
2. 菜单弹出后是否不会自动隐藏
3. SourcesFragment 弹窗弹出后是否不会自动隐藏
