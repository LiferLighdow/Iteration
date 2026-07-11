package com.liferlighdow.iteration

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.ui.IterationTheme
import com.liferlighdow.iteration.ui.LauncherScreen
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.viewmodel.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register Download Receiver
        val downloadFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                    val savedId = prefs.getLong("update_download_id", -1L)
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2L)
                    
                    if (savedId != -1L && savedId == downloadId) {
                        val viewModel = androidx.lifecycle.ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
                        viewModel.installDownloadedUpdate()
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            downloadFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
            val showStatusBar by viewModel.showStatusBar.collectAsState()
            val showNavigationBar by viewModel.showNavigationBar.collectAsState()
            val isLightWallpaper by viewModel.isLightWallpaper.collectAsState()
            val newVersion by viewModel.newVersionAvailable.collectAsState()
            val downloadUrl by viewModel.newVersionDownloadUrl.collectAsState()

            LaunchedEffect(showStatusBar, showNavigationBar, isLightWallpaper) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                
                // 根據桌布明暗設置狀態欄圖示顏色 (淺色桌布使用深色圖示，反之亦然)
                windowInsetsController.isAppearanceLightStatusBars = isLightWallpaper

                if (showStatusBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                }

                if (showNavigationBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                }
            }

            IterationTheme(themeMode = themeMode, isAmoledBlack = isAmoledBlack) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    LauncherScreen(
                        viewModel = viewModel,
                        onAppClick = { app ->
                            viewModel.launchApp(app)
                        },
                        onSettingsClick = {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        }
                    )

                    newVersion?.let { version ->
                        UpdateDialog(
                            version = version,
                            onDismiss = { viewModel.dismissUpdateDialog() },
                            onDownload = {
                                if (downloadUrl != null) {
                                    viewModel.startDownload(downloadUrl!!, version)
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/LiferLighdow/Iteration/releases/latest".toUri())
                                    startActivity(intent)
                                }
                                viewModel.dismissUpdateDialog()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(version: String, onDismiss: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = { Text(stringResource(R.string.update_dialog_message, version)) },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(stringResource(R.string.update_dialog_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_dialog_cancel))
            }
        }
    )
}
