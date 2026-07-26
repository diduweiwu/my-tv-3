# Implementation Plan - Fix ANR and Optimize Performance

The user reported an ANR in `MainActivity` with 99% CPU usage. Analysis suggests several blocking operations on the main thread and potential redundant heavy tasks.

## User Review Required

> [!IMPORTANT]
> The main cause of the ANR is likely the fuzzy matching logic in `MainViewModel.readEPG` which runs on the main thread with O(N*M) complexity and heavy regex operations.

## Proposed Changes

### Core Logic Optimization

#### [MODIFY] [MainViewModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MainViewModel.kt)
- Optimize `readEPG` by moving matching logic out of `Dispatchers.Main`.
- Precompute normalized channel names to avoid redundant regex operations in the nested loop.
- Use a `Job` to manage `updateEPG` tasks, ensuring only one EPG update runs at a time.
- Refactor `preloadLogo` to use `viewModelScope` instead of `GlobalScope` and optimize coroutine creation.

#### [MODIFY] [ModalFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ModalFragment.kt)
- Move QR code generation to a background thread using `lifecycleScope`.
- Prevent blocking the main thread during `onViewCreated`.

### Utility Optimization

#### [MODIFY] [QrCodeUtil.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/QrCodeUtil.kt)
- Ensure bitmap generation is as efficient as possible (the current implementation is mostly fine, but we will ensure it's called from background threads).

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors.
- (If available) Run unit tests for channel matching logic.

### Manual Verification
- Deploy to a device/emulator.
- Trigger "Remote Settings" or "Appreciate" in Settings to verify `ModalFragment` doesn't lag the UI.
- Check logs to ensure EPG updates and logo preloading run smoothly without blocking the main thread.
- Verify that multiple rapid channel switches don't trigger excessive background work.
