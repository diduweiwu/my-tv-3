# Walkthrough

## 需求1：切换分组后的聚焦定位逻辑

### 修改文件
`app/src/main/java/com/lizongying/mytv3/MenuFragment.kt`

### 修改内容
在 `onItemClicked(position: Int)` 方法中，更新了切换分组后的聚焦逻辑：

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

### 变更说明
- 移除了原来的 `groupAdapter.requestFocusToPosition(position)`（点击后聚焦到分组）
- 改为将焦点移到频道列表，并根据分组类型决定聚焦位置

---

## 需求2：去掉左侧频道分组和弹窗列表组件的自动隐藏逻辑

### 修改文件1
`app/src/main/java/com/lizongying/mytv3/MainActivity.kt`

### 修改内容
1. **删除 `delayHideMenu` 常量**
2. **清空 `menuActive()` 方法体**
3. **删除 `hideMenu` Runnable**
4. **`hideAllPopups()` 中移除 `handler.removeCallbacks(hideMenu)`**

```kotlin
// 修改前
fun menuActive() {
    handler.removeCallbacks(hideMenu)
    handler.postDelayed(hideMenu, delayHideMenu)
}

private val hideMenu = Runnable {
    if (!isFinishing && !supportFragmentManager.isStateSaved) {
        if (!menuFragment.isHidden) {
            supportFragmentManager.beginTransaction()
                .hide(menuFragment)
                .commitAllowingStateLoss()
        }
    }
}

// 修改后
fun menuActive() {
    // 已移除自动隐藏逻辑
}
```

---

### 修改文件2
`app/src/main/java/com/lizongying/mytv3/SourcesFragment.kt`

### 修改内容
1. **删除 `delayHideFragment` 常量**
2. **删除 `hideFragment` Runnable**
3. **移除 `onViewCreated` 中的 `handler.postDelayed(hideFragment, delayHideFragment)`**
4. **简化 `onKey` 方法，移除定时器重置逻辑**

```kotlin
// 修改前
override fun onKey(keyCode: Int, tag: String): Boolean {
    handler.removeCallbacks(hideFragment)
    handler.postDelayed(hideFragment, delayHideFragment)
    return false
}

// 修改后
override fun onKey(keyCode: Int, tag: String): Boolean {
    return false
}
```

---

## 验证结果
- 三个文件均无编译错误
- 所有修改符合需求描述
