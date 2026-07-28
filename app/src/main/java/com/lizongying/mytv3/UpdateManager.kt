package com.lizongying.mytv0

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

    private var downloadReceiver: BroadcastReceiver? = null
    var updateInfo: UpdateInfo? = null

    fun checkAndUpdate() {
        Log.i(TAG, "checkAndUpdate")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val info = fetchLatestRelease()
                if (info == null) {
                    updateUI("版本获取失败", false)
                    return@launch
                }

                updateInfo = info
                val currentVersionCode = getCurrentVersionCode(context)

                Log.i(TAG, "Remote: ${info.tagName} (${info.versionCode}), Current: $currentVersionCode")

                if (info.versionCode > currentVersionCode) {
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

    private suspend fun fetchLatestRelease(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", context.getString(R.string.app_name))

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

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
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
        try {
            val fileName = "my-tv-0-${updateInfo.versionName}.apk"
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.mkdirs()
            val apkFile = File(downloadDir, fileName)

            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("${context.getString(R.string.app_name)} ${updateInfo.versionName}")
                .setDescription("正在下载更新...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setMimeType("application/vnd.android.package-archive")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                    if (id == downloadId) {
                        val query = DownloadManager.Query().setFilterById(id)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                installApk(context, apkFile)
                            } else {
                                Log.i(TAG, "Download failure")
                            }
                        }
                        cursor.close()
                        context.unregisterReceiver(this)
                    }
                }
            }

            context.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (e: Exception) {
            Log.e(TAG, "startDownload error", e)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "installApk error", e)
        }
    }

    companion object {
        private const val TAG = "UpdateManager"
        private const val API_URL = "https://api.github.com/repos/diduweiwu/my-tv-3/releases/latest"
        private const val GH_PROXY = "https://gh-proxy.org/"
    }

    override fun onConfirm() {
        Log.i(TAG, "onConfirm $updateInfo")
        updateInfo?.let { startDownload(it) }
    }

    override fun onCancel() {
    }

    fun destroy() {
        if (downloadReceiver != null) {
            context.unregisterReceiver(downloadReceiver)
            Log.i(TAG, "destroy downloadReceiver")
        }
    }
}
