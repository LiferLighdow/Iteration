package com.liferlighdow.iteration.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.liferlighdow.iteration.R
import java.io.File

class UpdateManager(private val context: Context) {

    fun startDownload(url: String, version: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(context.getString(R.string.update_download_title, version))
            .setDescription(context.getString(R.string.update_download_description))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "iteration_update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("update_download_id", downloadId)
            .putString("update_version_downloading", version)
            .apply()
            
        Toast.makeText(context, R.string.update_download_started, Toast.LENGTH_SHORT).show()
    }

    fun installDownloadedUpdate(actionMode: ActionMode) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "iteration_update.apk")
        if (!file.exists()) {
            Toast.makeText(context, "Update file not found", Toast.LENGTH_SHORT).show()
            return
        }

        when (actionMode) {
            ActionMode.SHIZUKU -> {
                executeShizukuCommand(arrayOf("pm", "install", "-r", file.absolutePath), context)
            }
            ActionMode.ROOT -> {
                executeCommand(arrayOf("su", "-c", "pm install -r ${file.absolutePath}"), context)
            }
            else -> {
                // 普通模式：使用 Intent 安裝
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
