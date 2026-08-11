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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liferlighdow.iteration.ui.IterationTheme
import com.liferlighdow.iteration.ui.LauncherScreen
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.viewmodel.*

class MainActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.refreshPassword()
        if (viewModel.shouldRefreshIconsOnReturn) {
            viewModel.shouldRefreshIconsOnReturn = false
            viewModel.clearIconCache()
        }
        // 回到 Launcher 時執行綠化清理
        viewModel.performGreenifyCleanup()
    }

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
        enableEdgeToEdge(
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
            val showStatusBar by viewModel.showStatusBar.collectAsState()
            val showNavigationBar by viewModel.showNavigationBar.collectAsState()
            val isLightWallpaper by viewModel.isLightWallpaper.collectAsState()
            val density = LocalDensity.current
            val imeBottom = WindowInsets.ime.getBottom(density)
            val isImeVisible = imeBottom > 0
            val isMaterialYouEnabled by viewModel.isMaterialYouEnabled.collectAsState()
            val seedColor by viewModel.seedColor.collectAsState()
            val newVersion by viewModel.newVersionAvailable.collectAsState()
            val downloadUrl by viewModel.newVersionDownloadUrl.collectAsState()
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

            LaunchedEffect(showStatusBar, showNavigationBar, isLightWallpaper, lifecycleState, imeBottom) {
                if (lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect
                
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                
                // 根據桌布明暗設置狀態欄與導覽列圖示顏色
                windowInsetsController.isAppearanceLightStatusBars = isLightWallpaper
                windowInsetsController.isAppearanceLightNavigationBars = isLightWallpaper

                if (showStatusBar) {
                    windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                }

                if (showNavigationBar) {
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    // 對於無障礙模式，使用最穩定的黏性沉浸模式
                    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    
                    // 如果進階權限可用且非無障礙模式，LaunchEffect 就只作為 UI 同步。
                    // 核心隱藏由 setShowNavigationBar 中的 Shell 指令處理。
                }
            }

            IterationTheme(
                themeMode = themeMode,
                isAmoledBlack = isAmoledBlack,
                isMaterialYouEnabled = isMaterialYouEnabled,
                seedColor = seedColor
            ) {
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
