package com.liferlighdow.iteration

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.ui.IterationTheme
import com.liferlighdow.iteration.ui.LauncherScreen
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()
            IterationTheme(themeMode = themeMode, isAmoledBlack = isAmoledBlack) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    LauncherScreen(
                        viewModel = viewModel,
                        onAppClick = { app ->
                            if (app.isShortcut) {
                                viewModel.launchShortcut(app.packageName, app.shortcutId!!)
                            } else {
                                viewModel.logAppLaunch(app.packageName)
                                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                if (intent != null) startActivity(intent)
                            }
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
