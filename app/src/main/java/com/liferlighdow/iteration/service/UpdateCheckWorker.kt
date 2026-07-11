package com.liferlighdow.iteration.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val interval = prefs.getInt("update_check_interval", 0)
        
        if (interval == 0) return Result.success()

        try {
            val url = URL("https://api.github.com/repos/LiferLighdow/Iteration/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").removePrefix("v")
                
                val currentVersion = applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0.0.0"

                if (isNewerVersion(latestVersion, currentVersion)) {
                    val assets = json.optJSONArray("assets")
                    val downloadUrl = if (assets != null) getBestAbiMatch(assets) else null
                    
                    prefs.edit()
                        .putString("new_version_available", latestVersion)
                        .putString("new_version_download_url", downloadUrl)
                        .apply()
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateCheckWorker", "Error checking for updates", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun getBestAbiMatch(assets: JSONArray): String? {
        val deviceAbis = android.os.Build.SUPPORTED_ABIS
        
        // 優先順序：根據設備支援的 ABI 順序尋找
        for (abi in deviceAbis) {
            val searchPattern = when {
                abi.contains("arm64-v8a") -> "arm64-v8a"
                abi.contains("armeabi-v7a") -> "armeabi-v7a"
                abi.contains("x86_64") -> "x86_64"
                abi.contains("x86") -> "x86"
                else -> null
            }
            
            if (searchPattern != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name").lowercase()
                    if (name.contains(searchPattern) && name.endsWith(".apk")) {
                        return asset.getString("browser_download_url")
                    }
                }
            }
        }
        
        // 如果找不到精確匹配，找 universal 版本
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name").lowercase()
            if (name.contains("universal") && name.endsWith(".apk")) {
                return asset.getString("browser_download_url")
            }
        }
        return null
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
