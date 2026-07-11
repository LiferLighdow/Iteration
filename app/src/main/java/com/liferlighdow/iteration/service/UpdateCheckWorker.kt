package com.liferlighdow.iteration.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            // Using GitHub API for latest release. 
            // Repository: LiferLighdow/Iteration
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
                    prefs.edit().putString("new_version_available", latestVersion).apply()
                    // We might need a way to notify the UI. 
                    // Since the app might not be running, we just store it.
                    // When the app starts or if it is running, it can check this pref.
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateCheckWorker", "Error checking for updates", e)
            return Result.retry()
        }

        return Result.success()
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
