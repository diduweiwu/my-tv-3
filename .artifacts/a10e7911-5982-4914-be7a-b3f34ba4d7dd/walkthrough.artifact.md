# Bug Fix: Channel List Positioning and Persistent Highlight

I have fixed the issue where the channel list was not correctly positioning or highlighting the playing channel when re-opening the menu.

## Changes Made

### 1. Persistent Highlight in Channel List
- **Modified [ListAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ListAdapter.kt)**:
    - Added `activePosition` to track the playing channel even when the list doesn't have focus.
    - Updated `ViewHolder.focus()` to display a dim highlight for the `activePosition` when focus is elsewhere (e.g., on the group list).
    - Updated `update()` and `toPosition()` to synchronize `activePosition` with the currently playing channel.
    - Ensured `onBindViewHolder` correctly applies the highlight state.

### 2. Improved Positioning Logic
- **Modified [ListAdapter.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ListAdapter.kt)**:
    - Increased the delay in `toPosition()` from 50ms to 100ms to allow more time for the RecyclerView to finish layout before requesting focus.
    - Added `notifyDataSetChanged()` in `toPosition()` to ensure the highlight is updated immediately.
- **Modified [MenuFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/MenuFragment.kt)**:
    - Updated `onVisible()` to explicitly set `listAdapter.activePosition` before calling `toPosition()`. This ensures that even if the focus request takes a moment, the highlight is already correctly assigned.

## Verification

- **Sync Check**: Verified that `activePosition` is updated when switching groups or channels.
- **Focus Check**: Confirmed that the "dim highlight" (active but not focused) correctly indicates the playing channel when navigating the group list.

> [!NOTE]
> The channel list now behaves consistently with the group list, providing clear visual feedback about which channel is currently playing, regardless of where the focus is.
