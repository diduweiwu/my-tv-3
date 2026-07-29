import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lizongying.mytv0"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lizongying.mytv3"
        minSdk = 21
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()

    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val propFile = file("keystore.properties")
            if (propFile.exists()) {
                props.load(propFile.inputStream())
                storeFile = file(props["storeFile"] as String)
                storePassword = props["storePassword"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            } else {
                storeFile = file(System.getProperty("user.home") + "/.android/release.keystore")
                storePassword = System.getenv("STORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("KEY_ALIAS") ?: "mytv3"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Flag to enable support for the new language APIs
        // For AGP 4.1+
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi =
                output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier
            if (abi != null) {
                val abiMultiplier = mapOf(
                    "armeabi-v7a" to 1,
                    "arm64-v8a" to 2,
                    "x86" to 3,
                    "x86_64" to 4
                )[abi] ?: 0
                val baseVersionCode = variant.outputs.first().versionCode.get()
                output.versionCode.set(baseVersionCode + abiMultiplier)
            }
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease") {
        doLast {
            val outputDir = file("build/outputs/apk")
            if (outputDir.exists()) {
                outputDir.walkTopDown().filter { it.isFile && it.extension == "apk" }.forEach { file ->
                    val newName = file.name.replace("app-", "my-tv-3-${getVersionName()}-")
                    file.renameTo(file.parentFile.resolve(newName))
                }
            }
        }
    }
}

fun getTag(): String {
    return try {
        val process = Runtime.getRuntime().exec("git describe --tags --always")
        process.waitFor()
        process.inputStream.bufferedReader().use(BufferedReader::readText).trim().removePrefix("v")
    } catch (_: Exception) {
        ""
    }
}

fun getVersionCode(): Int {
    val tag = getTag()
    if (tag.isNotEmpty()) {
        val baseVersion = tag.split("-").firstOrNull() ?: tag
        val parts = baseVersion.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val build = parts.getOrNull(3)?.toIntOrNull() ?: 0
        return major * 1000000 + minor * 10000 + patch * 100 + build
    }
    val sdf = SimpleDateFormat("yyyyMMddHH", Locale.getDefault())
    val dateStr = sdf.format(Date())
    return dateStr.toIntOrNull() ?: 1000000
}

fun getVersionName(): String {
    return getTag().ifEmpty {
        // 无 git tag 时使用日期格式
        val sdf = SimpleDateFormat("yyyy.MM.dd-HH", Locale.getDefault())
        sdf.format(Date())
    }
}

dependencies {
    // For AGP 7.4+
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.datasource.rtmp)

    implementation(libs.nanohttpd)
    implementation(libs.zxing)
    implementation(libs.glide)

    implementation(libs.gson)
    implementation(libs.okhttp)

    implementation(libs.core.ktx)
    implementation(libs.coroutines)

    implementation(libs.constraintlayout)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)

    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))
}