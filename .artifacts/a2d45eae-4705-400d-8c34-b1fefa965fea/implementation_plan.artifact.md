# Separate Channel Number Display Implementation Plan

The user wants to separate the channel number from the channel details popup (InfoFragment) and display it independently in the bottom-right corner when switching channels.

## User Review Required

> [!IMPORTANT]
> The channel number currently appears in two places:
> 1. In the **bottom-center** info popup (`InfoFragment`).
> 2. In the **top-right** overlay (`ChannelFragment`) when enabled or during typing.
>
> This plan will:
> - Remove the channel number from the `InfoFragment` popup.
> - Add a standalone channel number box in the **bottom-right** corner within `InfoFragment`.
> - Move the `ChannelFragment` (used for typing and optional display) to the **bottom-right** corner as well to maintain consistency.

## Proposed Changes

### Resources
#### [NEW] [rounded_dark.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/drawable/rounded_dark.xml)
- Create a drawable with all corners rounded (4dp) and a dark blur background.

### UI Layouts

#### [MODIFY] [info.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/info.xml)
- Remove `channel_num` from the `info` `LinearLayout`.
- Reduce the `info` `LinearLayout` width from `480dp` to `400dp` (it will now only contain the channel title, description, and logo).
- Add a new `TextView` with ID `channel_num` outside the `info` layout, positioned at `layout_gravity="bottom|end"` with `20dp` margins.
- Use `rounded_dark.xml` as the background for this new standalone box.

#### [MODIFY] [channel.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/channel.xml)
- Change the `channel` `RelativeLayout` `layout_gravity` from `end|top` to `end|bottom`.
- Update `layout_marginTop` to `layout_marginBottom` and adjust `layout_marginEnd` to `20dp` to match the new corner position.
- (Optional) Adjust styling to match the new standalone look.

### Code Logic

#### [MODIFY] [InfoFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/InfoFragment.kt)
- Update `onCreateView` to set layout parameters for the new standalone `channel_num` view (scaling for different screens).
- Ensure `show(TVModel)` continues to update the channel number text.

#### [MODIFY] [ChannelFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv0/ChannelFragment.kt)
- Update `onCreateView` to handle the new `marginBottom` instead of `marginTop`.

## Verification Plan

### Manual Verification
1.  **Switch Channels (Up/Down)**: Verify the info popup appears at the bottom center (without the channel number) and the channel number appears in a box in the bottom-right corner.
2.  **Type Channel Number**: Verify the input feedback appears in the bottom-right corner.
3.  **Check Layout**: Ensure the new standalone channel number box is clearly visible and correctly positioned on various screen sizes (via `px2Px` scaling).
