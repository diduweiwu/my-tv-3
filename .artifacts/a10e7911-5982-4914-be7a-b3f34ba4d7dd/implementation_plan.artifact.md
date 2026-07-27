# Bug Fix Plan: Group Sync and Highlight Visibility

This plan addresses two bugs in the channel list (MenuFragment):
1.  **Group Sync Bug:** The group list does not correctly reset to the current channel's group upon re-entering the menu if the user previously navigated to another group.
2.  **Highlight Visibility Bug:** When navigating long lists, the selected item sometimes remains out of view or the focus request fails due to timing issues.

## Proposed Changes

### 1. Group Sync Fix

The `onVisible` method in `MenuFragment.kt` currently uses raw group indices from the ViewModel directly with the RecyclerView, which fails when the "All Channels" group is filtered out (when `SP.showAllChannels` is false). We need to map the raw index to the adapter position.

#### [MODIFY] [MenuFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MenuFragment.kt)
- Update `onVisible()` to correctly calculate the adapter position for the group list.
- Use a helper method or consistent logic for this mapping.

### 2. Highlight Visibility and Focus Reliability Fix

The focus request after scrolling often fails because the target item hasn't been laid out yet. We will improve this by adding a small delay or retrying.

#### [MODIFY] [ListAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ListAdapter.kt)
- Increase the delay in `toPosition()` to ensure the item is laid out.
- Alternatively, implement a listener or a more robust focus-after-scroll mechanism.

#### [MODIFY] [GroupAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/GroupAdapter.kt)
- Improve `scrollToPositionAndSelect()` reliability.
- Fix potential off-by-one errors in group position mapping.

## Verification Plan

### Manual Verification
- **Group Sync:** Open menu, move to a different group, exit menu. Re-open menu and verify it focuses the group of the currently playing channel.
- **Highlight Visibility:** Scroll down a long channel list (e.g., "All Channels") and verify that when focus is moved to/within the list, the selected item is always visible and correctly highlighted.
- **Filtering Toggle:** Test with `SP.showAllChannels` both true and false to ensure index mapping works in both cases.
