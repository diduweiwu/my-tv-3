# Implementation Plan - Fix UI Overlap and Unresponsiveness during Playback Error

The user reported that if a playback error occurs while the channel list (menu) is open, the menu is hidden or becomes unresponsive. This is likely due to the `ErrorFragment` being shown on top of the menu and potentially stealing focus or obscuring the view with its opaque black background.

## Analysis

1.  **Z-Order Race Condition**: In `MainActivity`, when an error occurs, `showFragment(errorFragment, ...)` is called. If the `ErrorFragment` is being added for the first time, it is added to the top of the `FrameLayout`. Although the code tries to `bringToFront()` the menus immediately after, the fragment transaction might not have completed yet, leaving the `ErrorFragment` on top.
2.  **Opaque Background**: `ErrorFragment` has a full-screen black background, which hides anything behind it.
3.  **Focus Management**: If `ErrorFragment` ends up on top, it might grab focus because its root view is focusable, preventing interaction with the menus behind it.

## Proposed Changes

### MainActivity

#### [MODIFY] [MainActivity.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainActivity.kt)
- Update `showFragment` to use `commitNowAllowingStateLoss()` where possible to ensure immediate view attachment.
- Implement an `ensureZOrder()` method that brings fragments to front in the correct order:
    - Layer 1 (Bottom): `PlayerFragment`, `ErrorFragment`
    - Layer 2: `LoadingFragment`
    - Layer 3: `InfoFragment`, `ChannelFragment`, `TimeFragment`
    - Layer 4 (Top): `MenuFragment`, `SettingFragment`, `ProgramFragment`
- Call `ensureZOrder()` inside `showFragment` and after error/loading state changes.
- In the error handling block, if a menu is visible, explicitly restore focus to it after showing the error fragment.

### Error Fragment

#### [MODIFY] [ErrorFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ErrorFragment.kt)
- Make the root view NOT focusable if possible, or only focusable if it's the primary interactive element (currently it has no buttons).
- Actually, it's better to keep it focusable but ensure it's at the back.

#### [MODIFY] [error.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/error.xml)
- Consider making the background semi-transparent if other overlays are present, or just rely on Z-order. (I'll stick to Z-order first as it's cleaner for TV).

## Verification Plan

### Manual Verification
- Open the channel list (Menu).
- Simulate/Trigger a playback error (e.g., by picking a broken link or disconnecting network).
- Verify that the error message appears but the channel list remains visible on top and remains interactive (can scroll, can pick another channel).
- Verify that backing out of the menu still shows the error fragment correctly.
