# Walkthrough - Configure Android TV Banner

I have updated the application banner for Android TV to use `banner0.png`.

## Changes

### Android Manifest

I modified [AndroidManifest.xml](file:///Users/itest/Code/my-tv-0/app/src/main/AndroidManifest.xml) to point the `android:banner` attribute to `@drawable/banner0`.

```diff
--- /Users/itest/Code/my-tv-0/app/src/main/AndroidManifest.xml
+++ /Users/itest/Code/my-tv-0/app/src/main/AndroidManifest.xml
@@ -7,7,7,7,7 @@
     <application
         android:name="com.lizongying.mytv3.MyTVApplication"
         android:allowBackup="true"
-        android:banner="@drawable/logo0"
+        android:banner="@drawable/banner0"
         android:icon="@drawable/logo0"
         android:label="@string/app_name"
```

## Verification Results

### Manual Verification
- Verified that `AndroidManifest.xml` now correctly references `@drawable/banner0` for the banner.
