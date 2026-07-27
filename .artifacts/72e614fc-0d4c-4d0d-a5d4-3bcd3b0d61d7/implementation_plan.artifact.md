# Prevent UI Components from being Hidden on Playback Error

The user reported that playback errors cause UI components (like the side menus) to be hidden or become unresponsive. This happens because the playback error handler shows a full-screen `ErrorFragment` with a black background, brings it to the front, and requests focus, effectively covering and interrupting any ongoing menu interactions.

## Proposed Changes

I will modify the error handling logic in `MainActivity` to ensure that if a menu is currently visible, the `ErrorFragment` does not steal focus or cover the menu.

### :app

#### [MODIFY] [MainActivity.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainActivity.kt)
- Update the `errInfo` observer in the `watch()` method.
- When an error occurs (`err != ""`):
    - Show the `ErrorFragment` as usual.
    - Check if any menu is visible using `isAnyMenuVisible()`.
    - If a menu is visible:
        - Skip `errorFragment.view?.requestFocus()`.
        - Explicitly bring the visible menu(s) to the front again to ensure they are not covered by the `ErrorFragment`.
    - If no menu is visible:
        - Proceed with `errorFragment.view?.requestFocus()`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure no build regressions.

### Manual Verification
- Deploy the app.
- Open the side menu (e.g., press Enter).
- Simulate a playback error (e.g., by disconnecting network or using a broken stream, if possible, or by inspecting the code logic).
- Verify that the menu remains visible and interactive even when the error message is shown in the background.
- Verify that if no menu is open, the error fragment correctly takes focus for accessibility/navigation.
