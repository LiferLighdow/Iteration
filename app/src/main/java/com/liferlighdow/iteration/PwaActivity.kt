package com.liferlighdow.iteration

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.liferlighdow.iteration.ui.IterationTheme
import java.io.File

class PwaActivity : ComponentActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val url = intent.getStringExtra("url") ?: "https://www.google.com"
        val label = intent.getStringExtra("label") ?: "PWA"
        val uniqueId = intent.getStringExtra("uniqueId") ?: ""

        // 設置多工介面 (Recents) 的圖示與標題
        setupTaskDescription(label, uniqueId)

        setContent {
            var webView by remember { mutableStateOf<WebView?>(null) }
            var customView by remember { mutableStateOf<View?>(null) }
            var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

            // 處理實體/手勢返回
            BackHandler {
                when {
                    customView != null -> {
                        // 退出全螢幕影片時恢復直向
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        customViewCallback?.onCustomViewHidden()
                        customView = null
                        customViewCallback = null
                    }
                    webView?.canGoBack() == true -> {
                        webView?.goBack()
                    }
                    else -> {
                        finish()
                    }
                }
            }

            // 檔案選擇器回調處理
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
                filePathCallback = null
            }

            IterationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize().statusBarsPadding(),
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this
                                    // 啟動硬體加速
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        loadsImagesAutomatically = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        javaScriptCanOpenWindowsAutomatically = true
                                        mediaPlaybackRequiresUserGesture = false
                                    }
                                    
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            return false 
                                        }
                                    }
                                    
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onReceivedTitle(view: WebView?, title: String?) {
                                            super.onReceivedTitle(view, title)
                                        }

                                        override fun onShowFileChooser(
                                            webView: WebView?,
                                            callback: ValueCallback<Array<Uri>>?,
                                            params: FileChooserParams?
                                        ): Boolean {
                                            this@PwaActivity.filePathCallback = callback
                                            filePickerLauncher.launch("image/*")
                                            return true
                                        }

                                        // 核心功能：支援影片全螢幕自動轉向
                                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                            if (customView != null) {
                                                callback?.onCustomViewHidden()
                                                return
                                            }
                                            // 進入全螢幕時轉向橫向
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            customView = view
                                            customViewCallback = callback
                                        }

                                        override fun onHideCustomView() {
                                            if (customView == null) return
                                            // 退出全螢幕時回歸直向
                                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            customView = null
                                            customViewCallback = null
                                        }
                                    }
                                    
                                    loadUrl(if (!url.startsWith("http")) "https://$url" else url)
                                }
                            }
                        )

                        // 全螢幕影片覆蓋層
                        if (customView != null) {
                            AndroidView(
                                factory = { 
                                    (customView?.parent as? ViewGroup)?.removeView(customView)
                                    customView!! 
                                },
                                modifier = Modifier.fillMaxSize().background(Color.Black)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 設置多工介面的外觀 (TaskDescription)
     * 從磁碟快取中讀取 PWA 圖示並套用
     */
    private fun setupTaskDescription(label: String, uniqueId: String) {
        try {
            val fileSafeId = uniqueId.replace("/", "_").replace(":", "_").replace("@", "_")
            // 嘗試從磁碟快取找圖示 (這裡假設我們已經生成過快取了)
            val cacheDir = File(filesDir, "processed_icons")
            val iconFile = cacheDir.listFiles()?.find { it.name.startsWith("${fileSafeId}_") }
            
            val iconBitmap = if (iconFile != null && iconFile.exists()) {
                BitmapFactory.decodeFile(iconFile.absolutePath)
            } else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ 使用繼承自 TaskDescription 的方式
                @Suppress("DEPRECATION")
                val td = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityManager.TaskDescription.Builder()
                        .setLabel(label)
                        .apply {
                            if (iconBitmap != null) {
                                // 修正 API 37 錯誤，改用標準的 Icon 物件
                                setIcon(android.graphics.drawable.Icon.createWithBitmap(iconBitmap))
                            }
                        }
                        .build()
                } else {
                    ActivityManager.TaskDescription(label, iconBitmap)
                }
                setTaskDescription(td)
            } else {
                @Suppress("DEPRECATION")
                setTaskDescription(ActivityManager.TaskDescription(label, iconBitmap))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
