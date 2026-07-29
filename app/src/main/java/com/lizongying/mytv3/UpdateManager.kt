package com.lizongying.mytv0

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNote: String
)

class UpdateManager(
    private var context: Context,
    private var versionCode: Long
) :
    ConfirmationFragment.ConfirmationListener {

    var updateInfo: UpdateInfo? = null
    var progressListener: ProgressListener? = null
    var isDownloading = false
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun checkAndUpdate() {
        Log.i(TAG, "checkAndUpdate")
        clearDownloadCache()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val info = fetchLatestRelease()
                if (info == null) {
                    updateUI("版本获取失败", false)
                    return@launch
                }

                updateInfo = info
                val currentVersionName = getVersionName(context)

                Log.i(TAG, "Remote: ${info.tagName}, Current: $currentVersionName")

                if (compareVersion(info.versionName, currentVersionName) > 0) {
                    updateUI("最新版本：${info.versionName}", true)
                } else {
                    updateUI("已是最新版本，不需要更新", false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error occurred: ${e.message}", e)
                updateUI("版本获取失败", false)
            }
        }
    }

    private fun compareVersion(v1: String, v2: String): Int {
        fun parse(v: String): List<Int> {
            val parts = v.trim()
                .removePrefix("v")
                .removePrefix("V")
                .split("-")
                .first()
                .split(".")
                .map { it.trim().toIntOrNull() ?: 0 }
            return parts + List(4 - parts.size) { 0 }.takeLast(maxOf(0, 4 - parts.size))
        }
        val parts1 = parse(v1)
        val parts2 = parse(v2)
        for (i in 0 until 4) {
            val a = parts1[i]
            val b = parts2[i]
            if (a != b) return a - b
        }
        return 0
    }

    private suspend fun fetchLatestRelease(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error: ${connection.responseCode}")
                    return@withContext null
                }

                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()
                connection.disconnect()

                val jsonObject = Gson().fromJson(response, JsonObject::class.java)
                val tagName = jsonObject.get("tag_name")?.asString ?: return@withContext null
                val releaseNote = jsonObject.get("body")?.asString ?: ""

                val remoteVersionCode = parseVersionCode(tagName)
                val downloadUrl = findApkDownloadUrl(jsonObject)
                if (downloadUrl == null) {
                    Log.e(TAG, "No APK download URL found")
                    return@withContext null
                }

                UpdateInfo(
                    tagName = tagName,
                    versionName = tagName,
                    versionCode = remoteVersionCode,
                    downloadUrl = "$GH_PROXY$downloadUrl",
                    releaseNote = releaseNote
                )
            } catch (e: Exception) {
                Log.e(TAG, "fetchLatestRelease error", e)
                null
            }
        }
    }

    private fun parseVersionCode(tagName: String): Int {
        val version = tagName.removePrefix("v").removePrefix("V")
        val parts = version.split(".")
        return try {
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val build = parts.getOrNull(3)?.toIntOrNull() ?: 0
            major * 1000000 + minor * 10000 + patch * 100 + build
        } catch (e: Exception) {
            0
        }
    }

    private fun getVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun findApkDownloadUrl(releaseJson: JsonObject): String? {
        val assets = releaseJson.getAsJsonArray("assets") ?: return null
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        val abiMapping = mapOf(
            "arm64-v8a" to "arm64-v8a",
            "armeabi-v7a" to "armeabi-v7a",
            "x86" to "x86",
            "x86_64" to "x86_64"
        )

        val targetAbi = abiMapping[abi] ?: "arm64-v8a"

        for (asset in assets) {
            val obj = asset.asJsonObject
            val name = obj.get("name")?.asString ?: continue
            val downloadUrl = obj.get("browser_download_url")?.asString ?: continue

            if (name.contains(targetAbi) && name.endsWith(".apk")) {
                return downloadUrl
            }
        }

        for (asset in assets) {
            val obj = asset.asJsonObject
            val name = obj.get("name")?.asString ?: continue
            if (name.contains("universal") && name.endsWith(".apk")) {
                return obj.get("browser_download_url")?.asString
            }
        }

        return null
    }

    private fun updateUI(text: String, update: Boolean) {
        val dialog = ConfirmationFragment(this@UpdateManager, text, update)
        dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, TAG)
    }

    private fun startDownload(updateInfo: UpdateInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "my-tv-0-${updateInfo.versionName}.apk"
                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                downloadDir?.mkdirs()
                val apkFile = File(downloadDir, fileName)

                if (apkFile.exists()) {
                    if (isFileComplete(apkFile)) {
                        Log.i(TAG, "APK already exists and is valid, installing directly")
                        withContext(Dispatchers.Main) {
                            isDownloading = false
                            progressListener?.onDownloadComplete()
                            installApk(context, apkFile)
                        }
                        return@launch
                    } else {
                        Log.w(TAG, "APK exists but invalid, deleting")
                        apkFile.delete()
                    }
                }

                // Step 1: Get file size and check Range support
                val headRequest = Request.Builder()
                    .url(updateInfo.downloadUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                    .build()

                val totalBytes = client.newCall(headRequest).execute().use { response ->
                    if (response.isSuccessful) response.body?.contentLength() ?: -1L else -1L
                }

                if (totalBytes <= 0) {
                    Log.w(TAG, "Failed to get content length, falling back to single thread")
                    downloadSingleThread(updateInfo, apkFile)
                    return@launch
                }

                Log.i(TAG, "Starting multi-threaded download: size=$totalBytes")
                val threadCount = 3
                val chunkSize = totalBytes / threadCount
                val totalRead = AtomicLong(0)
                var lastUpdateTime = 0L

                val deferreds = (0 until threadCount).map { i ->
                    val start = i * chunkSize
                    val end = if (i == threadCount - 1) totalBytes - 1 else (i + 1) * chunkSize - 1
                    
                    async {
                        val request = Request.Builder()
                            .url(updateInfo.downloadUrl)
                            .header("Range", "bytes=$start-$end")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                            .header("Accept", "*/*")
                            .header("Connection", "keep-alive")
                            .header("Referer", "https://github.com/")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw Exception("Range request failed: $response")
                            val body = response.body ?: throw Exception("Body is null")
                            
                            RandomAccessFile(apkFile, "rw").use { raf ->
                                raf.seek(start)
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        raf.write(buffer, 0, bytesRead)
                                        val currentTotal = totalRead.addAndGet(bytesRead.toLong())
                                        
                                        val now = System.currentTimeMillis()
                                        if (now - lastUpdateTime > 300) {
                                            lastUpdateTime = now
                                            val percent = (currentTotal * 100 / totalBytes).toInt()
                                            val downloadedMB = currentTotal / (1024 * 1024)
                                            val totalMB = totalBytes / (1024 * 1024)
                                            withContext(Dispatchers.Main) {
                                                progressListener?.onProgress("下载中 ${percent}% (${downloadedMB}MB/${totalMB}MB) [多线程]")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                deferreds.awaitAll()

                if (isFileComplete(apkFile, totalBytes)) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        progressListener?.onDownloadComplete()
                        installApk(context, apkFile)
                    }
                } else {
                    throw Exception("File size mismatch after multi-threaded download")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    progressListener?.onDownloadFailed()
                }
            }
        }
    }

    private suspend fun downloadSingleThread(updateInfo: UpdateInfo, apkFile: File) {
        try {
            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Referer", "https://github.com/")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val body = response.body ?: throw Exception("Response body is null")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 300) {
                                lastUpdate = now
                                val percent = if (totalBytes > 0) (totalRead * 100 / totalBytes).toInt() else 0
                                val downloadedMB = totalRead / (1024 * 1024)
                                val totalMB = totalBytes / (1024 * 1024)
                                withContext(Dispatchers.Main) {
                                    progressListener?.onProgress("下载中 ${percent}% (${downloadedMB}MB/${totalMB}MB)")
                                }
                            }
                        }
                    }
                }

                if (isFileComplete(apkFile, totalBytes)) {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        progressListener?.onDownloadComplete()
                        installApk(context, apkFile)
                    }
                } else {
                    throw Exception("File size mismatch after download")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Single thread download error", e)
            withContext(Dispatchers.Main) {
                isDownloading = false
                progressListener?.onDownloadFailed()
            }
        }
    }

    private fun isFileComplete(apkFile: File, expectedSize: Long = -1L): Boolean {
        if (!apkFile.exists()) return false
        if (apkFile.length() <= 0) return false
        if (expectedSize > 0 && apkFile.length() != expectedSize) {
            Log.w(
                TAG,
                "File size mismatch: expected=$expectedSize, actual=${apkFile.length()}"
            )
            return false
        }
        return isValidApk(apkFile)
    }

    private fun isValidApk(apkFile: File): Boolean {
        return try {
            apkFile.inputStream().use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4) return false
                // ZIP/APK magic bytes: 0x50 0x4B 0x03 0x04
                header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
            }
        } catch (e: Exception) {
            Log.e(TAG, "isValidApk error", e)
            false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "installApk error", e)
        }
    }

    private fun clearDownloadCache() {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        Log.i(TAG, "Deleting cached APK: ${file.name}")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearDownloadCache error", e)
        }
    }

    companion object {
        private const val TAG = "UpdateManager"
        private const val API_URL = "https://api.github.com/repos/diduweiwu/my-tv-3/releases/latest"
        private const val GH_PROXY = "https://gh-proxy.org/"
    }

    override fun onConfirm() {
        if (isDownloading) {
            Log.i(TAG, "Download already in progress, ignoring")
            return
        }
        Log.i(TAG, "onConfirm $updateInfo")
        isDownloading = true
        progressListener?.onDownloadStart()
        progressListener?.onProgress("准备下载...")
        Handler(Looper.getMainLooper()).postDelayed({
            updateInfo?.let { startDownload(it) }
        }, 300)
    }

    override fun onCancel() {
        progressListener?.onDownloadCanceled()
    }

    fun destroy() {
    }
}

interface ProgressListener {
    fun onProgress(text: String)
    fun onDownloadComplete()
    fun onDownloadFailed()
    fun onDownloadStart()
    fun onDownloadCanceled()
}
