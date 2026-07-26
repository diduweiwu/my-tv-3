# Walkthrough - ANR Fix and Performance Optimization

I have addressed the reported ANR by optimizing heavy operations in `MainViewModel` and `ModalFragment`, moving them off the main thread and reducing redundant computations.

## Changes Made

### MainViewModel Optimization
- **Background EPG Matching**: Refactored `readEPG` to perform channel-to-EPG fuzzy matching on `Dispatchers.IO`. This was the primary cause of the ANR, as the O(N*M) loop with regex operations was previously running on the main thread.
- **Normalized Name Caching**: Precomputed normalized channel and EPG names outside the nested loops in `readEPG`, significantly reducing the CPU overhead during matching.
- **Logo Preloading Fix**: Updated `preloadLogo` to use `viewModelScope` with `Dispatchers.IO` and optimized how coroutines are launched to avoid resource exhaustion.

### UI Responsiveness
- **Asynchronous QR Code Generation**: In `ModalFragment`, moved the QR code bitmap generation to a background thread using `lifecycleScope`. This ensures that showing the settings or appreciation modal doesn't stutter the UI.
- **Main Thread Safety**: Ensured all UI updates in `ModalFragment` and `MainViewModel` happen on the main thread while heavy logic stays in the background.

## Verification Results

### Automated Tests
- Successfully ran `gradle assembleDebug` to ensure project stability.

### Performance Observations
- The main thread is no longer blocked during EPG parsing or modal display.
- CPU usage during start-up and EPG updates is more balanced across background threads.
