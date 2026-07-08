package com.liferlighdow.iteration

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
            val showStatusBar by viewModel.showStatusBar.collectAsState()
            val showNavigationBar by viewModel.showNavigationBar.collectAsState()
            val isLightWallpaper by viewModel.isLightWallpaper.collectAsState()

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
                }
            }
        }
    }
}
