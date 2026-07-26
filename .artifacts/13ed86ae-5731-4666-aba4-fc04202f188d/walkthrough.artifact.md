# Walkthrough - Adjusted Appreciation Code Container Size

The height of the appreciation code container has been increased to 80% of the screen height to improve visibility, especially on TV devices.

## Changes

### [app](file:///Users/itest/Code/my-tv-0/app)

#### [ModalFragment.kt](file:///Users/itest/Code/my-tv-0/app/src/main/java/com/lizongying/mytv3/ModalFragment.kt)

- Added logic to calculate 80% of the screen height using `displayMetrics.heightPixels`.
- For the "Appreciate" modal (two side-by-side QR codes):
    - Set `modalImageContainer` height to the calculated target height.
    - Resized `modalImageLeft` and `modalImageRight` to fill the container height (minus margins).
- For single QR codes (URL-based):
    - Set the size to 80% of screen height when running on a TV.

#### [modal.xml](file:///Users/itest/Code/my-tv-0/app/src/main/res/layout/modal.xml)

- Changed `layout_width` and `layout_height` of the ImageViews to `wrap_content` to better accommodate programmatic resizing.

## Verification Results

### Automated Tests
- Ran `gradle assembleDebug` to ensure the project still compiles correctly. Status: **Success**.

### Manual Verification
- The changes are applied programmatically in `onViewCreated`, ensuring they adapt to the current screen dimensions at runtime.
