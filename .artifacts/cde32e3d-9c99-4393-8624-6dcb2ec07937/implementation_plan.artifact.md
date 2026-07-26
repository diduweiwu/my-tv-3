# Implementation Plan - Fix IndexOutOfBoundsException and LiveData Consistency

Address the `java.lang.IndexOutOfBoundsException` in `TVGroupModel.initTVGroup` caused by asynchronous LiveData updates, and ensure consistent state across the ViewModel and Model classes.

## User Review Required

> [!IMPORTANT]
> I will introduce a `safeSetValue` utility to handle LiveData updates. It will use `setValue()` when on the main thread (providing immediate state updates) and `postValue()` when on background threads. This resolves the race condition where `initTVGroup` was called before `addTVListModel` updates were applied.

## Proposed Changes

### Utilities

#### [MODIFY] [Ext.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/Ext.kt)
- Add `safeSetValue` extension function for `MutableLiveData`.

### Model Thread Safety & Consistency

#### [MODIFY] [TVGroupModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/models/TVGroupModel.kt)
- Use `safeSetValue` for all LiveData updates.
- Add safety check in `initTVGroup` to ensure `tvGroupValue` has enough items.

#### [MODIFY] [TVListModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/models/TVListModel.kt)
- Use `safeSetValue` for all LiveData updates.

#### [MODIFY] [TVModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/models/TVModel.kt)
- Use `safeSetValue` for all LiveData updates.

#### [MODIFY] [MainViewModel.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/MainViewModel.kt)
- Use `safeSetValue` for all LiveData updates.

## Verification Plan

### Manual Verification
- Deploy the app.
- Verify initial channel loading (which calls `addTVListModel` then `initTVGroup`).
- Verify web import functionality.
- Verify that the `IndexOutOfBoundsException` is no longer present in Logcat.
