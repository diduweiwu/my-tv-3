# Implementation Plan - Fix Gradle Configuration Cache Issue

The project is failing to build with the configuration cache enabled because it executes an external process (`git describe`) using `Runtime.getRuntime().exec()` during configuration time. This is unsupported by the Gradle configuration cache.

## Proposed Changes

### Build Configuration

#### [MODIFY] [app/build.gradle.kts](file:///Users/itest/Code/my-tv-0/app/build.gradle.kts)

- Replace the `Runtime.getRuntime().exec()` call in the `getTag()` function with `providers.exec`.
- This will allow Gradle to track the external process and make it compatible with the configuration cache.

```kotlin
fun getTag(): String {
    return try {
        providers.exec {
            commandLine("git", "describe", "--tags", "--always")
        }.standardOutput.asText.get().trim().removePrefix("v")
    } catch (_: Exception) {
        ""
    }
}
```

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug --configuration-cache` to verify that the build succeeds and the configuration cache is stored/reused without errors.

### Manual Verification
- Verify that `versionName` and `versionCode` are still correctly derived from the git tag.
