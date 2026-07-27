# Implementation Plan - Fix Channel List Positioning and Highlight

This plan addresses the issue where the channel list (right side) does not correctly position or highlight the currently playing channel when re-opening the menu.

## Proposed Changes

### 1. Enhanced Highlighting in ListAdapter

Currently, `ListAdapter` only shows a highlight when it has focus. We will add an `activePosition` field to track the "currently playing" or "last selected" channel, allowing it to remain highlighted even when the focus is on the group list.

#### [MODIFY] [ListAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ListAdapter.kt)
- Add `var activePosition: Int = -1`.
- Update `ViewHolder.focus(hasFocus: Boolean, isActive: Boolean = false)` to support an active background color (consistent with `GroupAdapter`).
- Update `onBindViewHolder` to use `activePosition` for highlighting.
- Update `onFocusChangeListener` to set `activePosition = position`.
- Improve `toPosition(position: Int)` to update `activePosition` and use more robust `postDelayed` logic for focus.
- Update `update(listTVModel: TVListModel)` to initialize `activePosition` from `listTVModel.positionPlayingValue`.

### 2. Synchronization in MenuFragment

#### [MODIFY] [MenuFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MenuFragment.kt)
- In `onVisible()`, explicitly set `listAdapter.activePosition` before calling `toPosition()`.
- Ensure `updateList()` correctly propagates the active position to the adapter.

### 3. Reliability Fixes in GroupAdapter

#### [MODIFY] [GroupAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/GroupAdapter.kt)
- Ensure `activePosition` is always updated on focus change.

## Verification Plan

### Manual Verification
1.  **Re-entry Test:** Play a channel, open the menu, move focus around, close the menu. Re-open the menu and verify both the group and the channel are correctly positioned and highlighted.
2.  **Highlight Test:** Verify that when focus is in the group list, the currently playing channel in the right list remains visible with a dim highlight.
3.  **Focus Test:** Verify that D-pad navigation correctly moves focus and scrolls both lists as expected.
