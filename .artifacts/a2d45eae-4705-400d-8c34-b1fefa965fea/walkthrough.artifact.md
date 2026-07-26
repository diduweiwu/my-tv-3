# Separate Channel Number Display Walkthrough

The channel number has been separated from the main info popup and moved to the bottom-right corner for a cleaner UI.

## Changes Made

### UI & Resources
- **New Drawable**: Created [rounded_dark.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/drawable/rounded_dark.xml) for a consistent standalone box style.
- **Info Layout**: Updated [info.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml) to move the channel number outside the main info container. It is now positioned at the bottom-right with a dedicated background.
- **Channel Typing Layout**: Updated [channel.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/channel.xml) to move the manual channel input indicator to the bottom-right, matching the new position of the automatic channel indicator.

### Logic & Styling
- **Dynamic Scaling**: Updated [InfoFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/InfoFragment.kt) and [ChannelFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/ChannelFragment.kt) to correctly scale the new layout margins and apply UI transparency (alpha) to the new standalone boxes.
- **Cleaned Up Indicators**: Simplified the channel typing display to focus purely on the number, ensuring it overlaps perfectly with the automatic indicator's position for a seamless transition.

## Verification Results

### Manual Verification Path
1. **Normal Channel Change**: The info popup appears at the bottom-center (logo + title + description). Simultaneously, the channel number (e.g., `001`) appears in a separate box at the bottom-right.
2. **Channel Selection**: When typing a number, it appears in the same bottom-right box.
3. **Transparency**: The new boxes correctly respect the global UI transparency settings.

render_diffs(file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml)
render_diffs(file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/channel.xml)
render_diffs(file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/InfoFragment.kt)
render_diffs(file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/ChannelFragment.kt)
