# Fix Configuration Cache Problem in app/build.gradle.kts

The project build fails when configuration cache is enabled because `app/build.gradle.kts` executes a `git` command using `Runtime.getRuntime().exec()`. This is not supported by Gradle's configuration cache as it's an untracked external process execution during the configuration phase.

## Proposed Changes

### [app module]

#### [MODIFY] [build.gradle.kts](file:///Users/itest/Code/my-tv-0/app/build.gradle.kts)
- Replace `Runtime.getRuntime().exec("git describe --tags --always")` with `providers.exec`.
- Use the Gradle Provider API to calculate `versionCode` and `versionName` based on the git tag.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:help --configuration-cache` to verify that the configuration cache can be stored without problems.
- Run `./gradlew :app:assembleDebug` to ensure the build still works and correctly calculates versions.

### Manual Verification
- Check that `versionCode` and `versionName` are still correctly derived from the git tag (if git is available in the environment).
