package com.liferlighdow.iteration.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.liferlighdow.iteration.R
import java.io.File

class IterationWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return IterationWallpaperEngine()
    }

    inner class IterationWallpaperEngine : Engine() {
        private var bitmap: Bitmap? = null
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.liferlighdow.iteration.ACTION_REFRESH_WALLPAPER") {
                    loadWallpaper()
                    draw()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter("com.liferlighdow.iteration.ACTION_REFRESH_WALLPAPER")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            loadWallpaper()
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {}
            try {
                bitmap?.recycle()
            } catch (e: Exception) {}
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                loadWallpaper()
                draw()
            }
        }

        private fun loadWallpaper() {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val wallpaperFile = File(documentsDir, "Iteration/Wallpaper/wallpaper.png")
                
                val newBitmap = if (wallpaperFile.exists()) {
                    BitmapFactory.decodeFile(wallpaperFile.absolutePath)
                } else {
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_builtin_wallpaper)?.let { drawable ->
                        val dm = applicationContext.resources.displayMetrics
                        com.liferlighdow.iteration.utils.WallpaperProcessor.drawableToCroppedBitmap(drawable, dm.widthPixels, dm.heightPixels)
                    }
                }
                
                val oldBitmap = bitmap
                bitmap = newBitmap
                if (oldBitmap != null && oldBitmap != newBitmap) {
                    try { oldBitmap.recycle() } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun draw() {
            val holder = surfaceHolder ?: return
            val canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                val bmp = bitmap
                if (bmp != null && !bmp.isRecycled) {
                    val canvasWidth = canvas.width.toFloat()
                    val canvasHeight = canvas.height.toFloat()
                    val bmpWidth = bmp.width.toFloat()
                    val bmpHeight = bmp.height.toFloat()

                    val scale = maxOf(canvasWidth / bmpWidth, canvasHeight / bmpHeight)
                    val scaledWidth = bmpWidth * scale
                    val scaledHeight = bmpHeight * scale
                    val left = (canvasWidth - scaledWidth) / 2f
                    val top = (canvasHeight - scaledHeight) / 2f

                    canvas.drawBitmap(bmp, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {}
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }
    }
}
