package com.lizongying.mytv0

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import com.bumptech.glide.Glide
import com.lizongying.mytv0.Utils.getUrls
import com.lizongying.mytv0.requests.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap


class ImageHelper(private val context: Context) {
    private val cacheDir = context.cacheDir

    private var dir: File = File(cacheDir, LOGO)
    private val files = ConcurrentHashMap<String, File>()

    init {
        if (!dir.exists()) {
            dir.mkdir()
        }
        dir.listFiles()?.forEach { file ->
            files[file.name] = file
        }
    }

    private suspend fun downloadImage(url: String, file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .build()

                HttpClient.okHttpClient.newCall(request).execute().use { response ->
                    Log.i(TAG, "downloadImage response: code=${response.code}, type=${response.header("Content-Type")}, url=$url")
                    if (!response.isSuccessful) {
                        Log.e(TAG, "downloadImage failed: HTTP ${response.code} for $url")
                        return@withContext false
                    }
                    val body = response.bodyAlias()
                    if (body == null) {
                        Log.e(TAG, "downloadImage failed: null body for $url")
                        return@withContext false
                    }
                    val bytes = body.bytes()
                    Log.i(TAG, "downloadImage downloaded ${bytes.size} bytes for $url")
                    if (bytes.size < 100) {
                        Log.e(TAG, "downloadImage failed: file too small (${bytes.size} bytes), probably not an image")
                        return@withContext false
                    }
                    file.writeBytes(bytes)
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "downloadImage error $url", e)
                false
            }
        }
    }

    suspend fun preloadImage(
        key: String,
        urlList: List<String>,
        logoUrl: String = "",
    ) {
        // 计算与 loadImage() 相同的缓存 key
        val logoFileName = if (logoUrl.isNotEmpty()) {
            logoUrl.substringAfterLast("/")
        } else {
            "${key.uppercase()}.png"
        }
        val cacheKey = "gitee_$logoFileName"

        val file = files[cacheKey]
        if (file != null) {
            Log.d(TAG, "image exists ${file.absolutePath}")
            return
        }

        if (urlList.isEmpty()) {
            return
        }

        val cacheFile = File(cacheDir, "$LOGO/$cacheKey")
        for (url in urlList) {
            if (downloadImage(url, cacheFile)) {
                if (cacheFile.length() > 100) {
                    files[cacheKey] = cacheFile
                    Log.d(TAG, "preloadImage success $url ${cacheFile.absolutePath}")
                    break
                } else {
                    Log.e(TAG, "preloadImage file too small: ${cacheFile.length()} bytes from $url")
                    cacheFile.delete()
                }
            }
        }
    }

    /**
     * 检查缓存是否存在
     */
    fun isCached(cacheKey: String): Boolean {
        return files[cacheKey] != null
    }

    /**
     * 只从缓存加载 logo，不进行网络请求。
     * 缓存不存在时显示空白透明占位图。
     */
    fun loadImage(
        key: String,
        imageView: androidx.appcompat.widget.AppCompatImageView,
        logoUrl: String = "",
    ) {
        Log.i(TAG, "loadImage called: key=$key, logoUrl=$logoUrl")

        // 清空旧 logo，避免换台后仍显示之前频道的 logo
        imageView.setImageDrawable(null)
        Glide.with(context).clear(imageView)

        // 从 tvg-logo URL 中截取文件名（如 CCTV17.png）
        val logoFileName = if (logoUrl.isNotEmpty()) {
            val name = logoUrl.substringAfterLast("/")
            Log.i(TAG, "extracted logoFileName from logoUrl: $name")
            name
        } else {
            val name = "${key.uppercase()}.png"
            Log.i(TAG, "using key as logoFileName: $name")
            name
        }

        // 用 logo 文件名作为缓存 key
        val cacheKey = "gitee_$logoFileName"
        val cacheFile = File(cacheDir, "$LOGO/$cacheKey")

        // 检查缓存文件是否存在且有效
        if (cacheFile.exists() && cacheFile.length() > 100) {
            Log.i(TAG, "cache hit: ${cacheFile.absolutePath}, size=${cacheFile.length()}")
            try {
                Glide.with(context)
                    .load(cacheFile)
                    .fitCenter()
                    .into(imageView)
            } catch (e: Exception) {
                Log.e(TAG, "cache load failed", e)
                imageView.setImageDrawable(null)
            }
        } else {
            // 缓存不存在，显示空白透明占位图
            Log.i(TAG, "cache miss: $logoFileName, show blank placeholder")
            imageView.setImageDrawable(null)
        }
    }

    fun clearImage() {
        val dir = File(cacheDir, LOGO)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    companion object {
        const val TAG = "ImageHelper"
        const val LOGO = "logo"
    }
}
