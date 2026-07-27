# Walkthrough - Fixed UI Overlap and Focus during Playback Error

I have fixed the issue where the user interface would become unresponsive or hidden when a playback error occurred while the channel list (menu) was open.

## Changes

### MainActivity
I implemented a robust Z-order management system and focus restoration logic in [MainActivity.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainActivity.kt).

1.  **Centralized Z-Order Management**: Added `ensureZOrder()` to explicitly define the stacking order of fragments. Menus are always placed at the top layer.
2.  **Synchronous Fragment Transactions**: Updated `showFragment` to use `commitNowAllowingStateLoss()` ensuring immediate attachment and layer adjustment.
3.  **Focus Restoration**: Added `restoreMenuFocus()` to ensure that if a menu was active when an error occurred, the focus is immediately returned to it so the user can continue navigating.
4.  **Error Handling Update**: Modified the error observer to call `ensureZOrder()` and `restoreMenuFocus()` when a menu is visible.

```kotlin
    private fun ensureZOrder() {
        val fragments = listOf(
            playerFragment,
            errorFragment,
            loadingFragment,
            infoFragment,
            channelFragment,
            timeFragment,
            menuFragment,
            settingFragment,
            programFragment
        )
        fragments.forEach {
            if (it.isAdded && !it.isHidden) {
                it.view?.bringToFront()
            }
        }
    }
```

## Verification Results

### Build Verification
- Successfully compiled the project with `:app:assembleDebug`.

### Logical Verification
- The layer management ensures that `ErrorFragment` (with its black background) is placed behind active menus, preventing it from obscuring the channel list or stealing focus.
