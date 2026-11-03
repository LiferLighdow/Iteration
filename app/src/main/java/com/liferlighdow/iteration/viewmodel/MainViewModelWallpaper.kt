package com.liferlighdow.iteration.viewmodel

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.EmojiPatternStyle
import com.liferlighdow.iteration.data.EmojiRenderingMode
import com.liferlighdow.iteration.data.WallpaperConfig
import com.liferlighdow.iteration.data.WallpaperPreset
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.ui.drawEmojiPattern
import com.liferlighdow.iteration.ui.parseEmojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream

private fun MainViewModel.notifyWallpaperChanged() {
    getApplication<android.app.Application>().sendBroadcast(Intent("com.liferlighdow.iteration.ACTION_REFRESH_WALLPAPER"))
}

fun MainViewModel.refreshWallpaperFromPrefs() {
    viewModelScope.launch(Dispatchers.IO) {
        val mType = prefs.getString("active_wallpaper_media_type", "IMAGE") ?: "IMAGE"
        val mPath = prefs.getString("active_wallpaper_media_path", null)?.ifEmpty { null }
        val presetName = prefs.getString("current_wallpaper_preset", "") ?: ""

        withContext(Dispatchers.Main) {
            _activeWallpaperMediaType.value = mType
            _activeWallpaperMediaPath.value = mPath
            _currentWallpaperPresetName.value = presetName
        }

        updateBlurredWallpaper(force = true)
    }
}

fun MainViewModel.updateBlurredWallpaper(force: Boolean = false) {
    if (!force && _blurredWallpaper.value != null && _rawWallpaper.value != null) return
    viewModelScope.launch(Dispatchers.IO) {
        val result = if (wallpaperFile.exists()) {
            wallpaperProcessor.loadWallpaperFromFile(wallpaperFile)
        } else {
            wallpaperProcessor.extractSystemWallpaper()
        }

        withContext(Dispatchers.Main) {
            result?.let {
                _rawWallpaper.value = it.raw
                _blurredWallpaper.value = it.blurred
                _isLightWallpaper.value = it.isLightWallpaper
                _wallpaperUpdateSignal.value = System.currentTimeMillis()
            }
        }
    }
}

fun MainViewModel.setCustomWallpaper(bitmap: Bitmap) {
    viewModelScope.launch(Dispatchers.IO) {
        _isApplyingWallpaper.value = true
        try {
            FileOutputStream(wallpaperFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            updateBlurredWallpaper(force = true)
            notifyWallpaperChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.forceSyncSystemWallpaper() {
    viewModelScope.launch(Dispatchers.IO) {
        _isApplyingWallpaper.value = true
        try {
            if (wallpaperFile.exists()) wallpaperFile.delete()
            updateBlurredWallpaper(force = true)
            notifyWallpaperChanged()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.loadWallpaperPresets() {
    viewModelScope.launch(Dispatchers.IO) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val wallpaperDir = File(documentsDir, "Iteration/Wallpaper")

        val builtinPresets = listOf(
            WallpaperPreset(name = "Builtin 1", folderPath = "", previewPath = null, wallpaperPath = "", originalPath = null, config = null, resId = R.drawable.ic_builtin_wallpaper),
            WallpaperPreset(name = "Builtin 2", folderPath = "", previewPath = null, wallpaperPath = "", originalPath = null, config = null, resId = R.drawable.ic_builtin_wallpaper2),
            WallpaperPreset(name = "Builtin 3", folderPath = "", previewPath = null, wallpaperPath = "", originalPath = null, config = null, resId = R.drawable.ic_builtin_wallpaper3),
            WallpaperPreset(name = "Builtin 4", folderPath = "", previewPath = null, wallpaperPath = "", originalPath = null, config = null, resId = R.drawable.ic_builtin_wallpaper4),
            WallpaperPreset(name = "Builtin 5", folderPath = "", previewPath = null, wallpaperPath = "", originalPath = null, config = null, resId = R.drawable.ic_builtin_wallpaper5)
        )

        val userPresets = if (wallpaperDir.exists()) {
            wallpaperDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { folder ->
                val configFile = File(folder, "config.json")
                val wallpaperFile = File(folder, "wallpaper.png")
                val previewFile = File(folder, "preview.jpg")
                val originalFile = File(folder, "original.jpg").takeIf { it.exists() } ?: File(folder, "original.png")

                if (!wallpaperFile.exists() && !configFile.exists()) return@mapNotNull null

                val config = if (configFile.exists()) {
                    try {
                        Json.decodeFromString<WallpaperConfig>(configFile.readText())
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val videoFile = File(folder, "media.mp4")
                val gifFile = File(folder, "media.gif")

                val mediaType = config?.mediaType ?: when {
                    videoFile.exists() -> "VIDEO"
                    gifFile.exists() -> "GIF"
                    else -> "IMAGE"
                }

                val mediaPath = config?.mediaPath ?: when (mediaType) {
                    "VIDEO" -> if (videoFile.exists()) videoFile.absolutePath else null
                    "GIF" -> if (gifFile.exists()) gifFile.absolutePath else null
                    else -> null
                }

                WallpaperPreset(
                    name = folder.name,
                    folderPath = folder.absolutePath,
                    previewPath = if (previewFile.exists()) previewFile.absolutePath else null,
                    wallpaperPath = wallpaperFile.absolutePath,
                    originalPath = if (originalFile.exists()) originalFile.absolutePath else null,
                    config = config,
                    mediaType = mediaType,
                    mediaPath = mediaPath,
                    resId = null
                )
            }?.sortedByDescending { File(it.folderPath).lastModified() } ?: emptyList()
        } else emptyList()

        _wallpaperPresets.value = builtinPresets + userPresets
    }
}

fun MainViewModel.applyWallpaperPreset(preset: WallpaperPreset, destinationFlags: Int = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) {
    viewModelScope.launch(Dispatchers.IO) {
        _isApplyingWallpaper.value = true
        try {
            val wm = WallpaperManager.getInstance(getApplication())
            val config = preset.config
            val mode = try { EmojiRenderingMode.valueOf(config?.renderingMode ?: "FULL") } catch(e: Exception) { EmojiRenderingMode.FULL }
            
            val wallpaperFileInPreset = File(preset.wallpaperPath)
            
            if ((destinationFlags and WallpaperManager.FLAG_SYSTEM) != 0) {
                config?.let { cfg ->
                    withContext(Dispatchers.Main) {
                        cfg.blur?.let { setLiquidGlassBlur(it) }
                        cfg.refractionHeight?.let { setLiquidGlassRefractionHeight(it) }
                        cfg.refractionAmount?.let { setLiquidGlassRefractionAmount(it) }
                        cfg.chromaticAberration?.let { setLiquidGlassChromaticAberration(it) }
                        cfg.themeMode?.let { try { setThemeMode(ThemeMode.valueOf(it)) } catch(e: Exception) {} }
                        cfg.isMaterialYou?.let { setMaterialYouEnabled(it) }
                        cfg.wallpaperColor?.let { setCustomWallpaperColor(it) }
                        cfg.emojiText?.let { setEmojiWallpaperText(if (mode == EmojiRenderingMode.FULL) "" else it) }
                        cfg.emojiPatternStyle?.let { try { setEmojiPatternStyle(EmojiPatternStyle.valueOf(it)) } catch(e: Exception) {} }
                    }
                }
            }

            val context = getApplication<android.app.Application>()
            val bitmapToApply = if (preset.resId != null) {
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, preset.resId)
                val dm = context.resources.displayMetrics
                if (drawable != null) {
                    com.liferlighdow.iteration.utils.WallpaperProcessor.drawableToCroppedBitmap(drawable, dm.widthPixels, dm.heightPixels)
                } else null
            } else if (wallpaperFileInPreset.exists()) {
                BitmapFactory.decodeFile(preset.wallpaperPath)
            } else null

            val color = config?.wallpaperColor ?: 0xFF2196F3.toInt()
            val liteBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            
            when (mode) {
                EmojiRenderingMode.LITE -> {
                    if ((destinationFlags and WallpaperManager.FLAG_SYSTEM) != 0) {
                        wm.setBitmap(liteBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        FileOutputStream(wallpaperFile).use { liteBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    if ((destinationFlags and WallpaperManager.FLAG_LOCK) != 0) {
                        wm.setBitmap(liteBitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                }
                EmojiRenderingMode.BALANCE -> {
                    if (bitmapToApply != null && (destinationFlags and WallpaperManager.FLAG_LOCK) != 0) {
                        wm.setBitmap(bitmapToApply, null, true, WallpaperManager.FLAG_LOCK)
                    }
                    if ((destinationFlags and WallpaperManager.FLAG_SYSTEM) != 0) {
                        wm.setBitmap(liteBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        FileOutputStream(wallpaperFile).use { liteBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                EmojiRenderingMode.FULL -> {
                    if (bitmapToApply != null) {
                        if ((destinationFlags and WallpaperManager.FLAG_SYSTEM) != 0) {
                            wm.setBitmap(bitmapToApply, null, true, WallpaperManager.FLAG_SYSTEM)
                            FileOutputStream(wallpaperFile).use { bitmapToApply.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        }
                        if ((destinationFlags and WallpaperManager.FLAG_LOCK) != 0) {
                            wm.setBitmap(bitmapToApply, null, true, WallpaperManager.FLAG_LOCK)
                        }
                    }
                }
            }

            if ((destinationFlags and WallpaperManager.FLAG_SYSTEM) != 0) {
                _currentWallpaperPresetName.value = preset.name
                prefs.edit().putString("current_wallpaper_preset", preset.name).apply()

                val mType = preset.mediaType
                val mPath = preset.mediaPath
                withContext(Dispatchers.Main) {
                    _activeWallpaperMediaType.value = mType
                    _activeWallpaperMediaPath.value = mPath
                }
                prefs.edit().putString("active_wallpaper_media_type", mType).apply()
                prefs.edit().putString("active_wallpaper_media_path", mPath ?: "").apply()

                updateBlurredWallpaper()
                notifyWallpaperChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.addNewWallpaperPreset(croppedBitmap: Bitmap, name: String, originalBitmap: Bitmap? = null) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val wallpaperDir = File(documentsDir, "Iteration/Wallpaper/$name")
            if (!wallpaperDir.exists()) wallpaperDir.mkdirs()

            val wallpaperFile = File(wallpaperDir, "wallpaper.png")
            val previewFile = File(wallpaperDir, "preview.jpg")
            val configFile = File(wallpaperDir, "config.json")
            val originalFile = File(wallpaperDir, "original.jpg")

            FileOutputStream(wallpaperFile).use { croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            originalBitmap?.let { 
                try {
                    FileOutputStream(originalFile).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val previewBitmap = if (croppedBitmap.width > 400) {
                Bitmap.createScaledBitmap(croppedBitmap, croppedBitmap.width / 4, croppedBitmap.height / 4, true)
            } else croppedBitmap
            FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }

            val currentConfig = WallpaperConfig(
                blur = _liquidGlassBlur.value,
                refractionHeight = _liquidGlassRefractionHeight.value,
                refractionAmount = _liquidGlassRefractionAmount.value,
                chromaticAberration = _liquidGlassChromaticAberration.value,
                themeMode = _themeMode.value.name,
                isMaterialYou = _isMaterialYouEnabled.value,
                wallpaperColor = _customWallpaperColor.value,
                emojiText = _emojiWallpaperText.value,
                emojiPatternStyle = _emojiPatternStyle.value.name
            )
            configFile.writeText(Json.encodeToString(currentConfig))
            loadWallpaperPresets()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun MainViewModel.deleteWallpaperPreset(preset: WallpaperPreset) {
    viewModelScope.launch(Dispatchers.IO) {
        val folder = File(preset.folderPath)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        if (_currentWallpaperPresetName.value == preset.name) {
            _currentWallpaperPresetName.value = ""
            prefs.edit().remove("current_wallpaper_preset").apply()
        }
        loadWallpaperPresets()
    }
}

fun MainViewModel.renameWallpaperPreset(preset: WallpaperPreset, newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val oldFolder = File(preset.folderPath)
        val newFolder = File(oldFolder.parentFile, newName)
        if (oldFolder.exists() && !newFolder.exists()) {
            oldFolder.renameTo(newFolder)
            if (_currentWallpaperPresetName.value == preset.name) {
                _currentWallpaperPresetName.value = newName
                prefs.edit().putString("current_wallpaper_preset", newName).apply()
            }
            loadWallpaperPresets()
        }
    }
}

fun MainViewModel.updateWallpaperPresetCrop(preset: WallpaperPreset, newCroppedBitmap: Bitmap) {
    viewModelScope.launch(Dispatchers.IO) {
        val wallpaperFile = File(preset.wallpaperPath)
        val previewFile = File(preset.folderPath, "preview.jpg")
        
        FileOutputStream(wallpaperFile).use { newCroppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        
        val previewBitmap = Bitmap.createScaledBitmap(newCroppedBitmap, newCroppedBitmap.width / 4, newCroppedBitmap.height / 4, true)
        FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        
        if (_currentWallpaperPresetName.value == preset.name) {
            withContext(Dispatchers.Main) { applyWallpaperPreset(preset) }
        } else {
            loadWallpaperPresets()
        }
    }
}

fun MainViewModel.addNewEmojiWallpaperPreset(mode: EmojiRenderingMode, color: Int, emojiText: String, style: EmojiPatternStyle, name: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val dm = getApplication<android.app.Application>().resources.displayMetrics
            val w = dm.widthPixels.coerceAtLeast(1080)
            val h = dm.heightPixels.coerceAtLeast(1920)
            
            val fullBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fullBitmap)
            canvas.drawColor(color)
            val emojis = parseEmojis(emojiText)
            if (emojis.isNotEmpty()) {
                drawEmojiPattern(canvas, emojis, w, h, style)
            }

            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val wallpaperDir = File(documentsDir, "Iteration/Wallpaper/$name")
            if (!wallpaperDir.exists()) wallpaperDir.mkdirs()

            val previewFile = File(wallpaperDir, "preview.jpg")
            val previewBitmap = Bitmap.createScaledBitmap(fullBitmap, w / 4, h / 4, true)
            FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }

            if (mode != EmojiRenderingMode.LITE) {
                val wallpaperFile = File(wallpaperDir, "wallpaper.png")
                FileOutputStream(wallpaperFile).use { fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } else {
                val wallpaperFile = File(wallpaperDir, "wallpaper.png")
                val lite = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
                FileOutputStream(wallpaperFile).use { lite.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }

            val configFile = File(wallpaperDir, "config.json")
            val config = WallpaperConfig(
                wallpaperColor = color,
                emojiText = emojiText,
                emojiPatternStyle = style.name,
                renderingMode = mode.name,
                themeMode = _themeMode.value.name,
                isMaterialYou = _isMaterialYouEnabled.value
            )
            configFile.writeText(Json.encodeToString(config))
            
            loadWallpaperPresets()
            
            withContext(Dispatchers.Main) {
                val presets = _wallpaperPresets.value
                val newlyCreated = presets.find { it.name == name }
                newlyCreated?.let { applyWallpaperPreset(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}



fun MainViewModel.addNewMediaWallpaperPreset(uri: android.net.Uri, name: String) {
    viewModelScope.launch(Dispatchers.IO) {
        _isApplyingWallpaper.value = true
        try {
            val context = getApplication<android.app.Application>()
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val uriString = uri.toString().lowercase()

            val isVideo = mimeType.startsWith("video/") || uriString.endsWith(".mp4") || uriString.endsWith(".m4v") || uriString.endsWith(".mkv") || uriString.endsWith(".webm")
            val isGif = mimeType.contains("gif", ignoreCase = true) || uriString.endsWith(".gif")

            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val wallpaperDir = File(documentsDir, "Iteration/Wallpaper/$name")
            if (!wallpaperDir.exists()) wallpaperDir.mkdirs()

            val previewFile = File(wallpaperDir, "preview.jpg")
            val wallpaperFile = File(wallpaperDir, "wallpaper.png")
            val configFile = File(wallpaperDir, "config.json")

            var mediaType = "IMAGE"
            var mediaPath: String? = null

            if (isVideo) {
                mediaType = "VIDEO"
                val mediaFile = File(wallpaperDir, "media.mp4")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(mediaFile).use { output ->
                        input.copyTo(output)
                    }
                }
                mediaPath = mediaFile.absolutePath

                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(mediaFile.absolutePath)
                    val frame = retriever.getFrameAtTime(0L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    frame?.let { b ->
                        FileOutputStream(wallpaperFile).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val previewBitmap = if (b.width > 400) Bitmap.createScaledBitmap(b, b.width / 4, b.height / 4, true) else b
                        FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            } else if (isGif) {
                mediaType = "GIF"
                val mediaFile = File(wallpaperDir, "media.gif")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(mediaFile).use { output ->
                        input.copyTo(output)
                    }
                }
                mediaPath = mediaFile.absolutePath

                try {
                    val bitmap = BitmapFactory.decodeFile(mediaFile.absolutePath)
                    bitmap?.let { b ->
                        FileOutputStream(wallpaperFile).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val previewBitmap = if (b.width > 400) Bitmap.createScaledBitmap(b, b.width / 4, b.height / 4, true) else b
                        FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                bitmap?.let { b ->
                    FileOutputStream(wallpaperFile).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val previewBitmap = if (b.width > 400) Bitmap.createScaledBitmap(b, b.width / 4, b.height / 4, true) else b
                    FileOutputStream(previewFile).use { previewBitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                }
            }

            val config = WallpaperConfig(
                blur = _liquidGlassBlur.value,
                themeMode = _themeMode.value.name,
                isMaterialYou = _isMaterialYouEnabled.value,
                mediaType = mediaType,
                mediaPath = mediaPath
            )
            configFile.writeText(Json.encodeToString(config))

            loadWallpaperPresets()

            withContext(Dispatchers.Main) {
                val presets = _wallpaperPresets.value
                val newlyCreated = presets.find { it.name == name }
                newlyCreated?.let { applyWallpaperPreset(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}
