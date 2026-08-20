package com.liferlighdow.iteration.viewmodel

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Environment
import androidx.lifecycle.viewModelScope
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

fun MainViewModel.updateBlurredWallpaper() {
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
            updateBlurredWallpaper()
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
            updateBlurredWallpaper()
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
        if (!wallpaperDir.exists()) return@launch

        val presets = wallpaperDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { folder ->
            val configFile = File(folder, "config.json")
            val wallpaperFile = File(folder, "wallpaper.png")
            val previewFile = File(folder, "preview.jpg")
            val originalFile = File(folder, "original.png")

            if (!wallpaperFile.exists() && !configFile.exists()) return@mapNotNull null

            val config = if (configFile.exists()) {
                try {
                    Json.decodeFromString<WallpaperConfig>(configFile.readText())
                } catch (e: Exception) {
                    null
                }
            } else null

            WallpaperPreset(
                name = folder.name,
                folderPath = folder.absolutePath,
                previewPath = if (previewFile.exists()) previewFile.absolutePath else null,
                wallpaperPath = wallpaperFile.absolutePath,
                originalPath = if (originalFile.exists()) originalFile.absolutePath else null,
                config = config
            )
        }?.sortedByDescending { File(it.folderPath).lastModified() } ?: emptyList()

        _wallpaperPresets.value = presets
    }
}

fun MainViewModel.applyWallpaperPreset(preset: WallpaperPreset) {
    viewModelScope.launch(Dispatchers.IO) {
        _isApplyingWallpaper.value = true
        try {
            val wm = WallpaperManager.getInstance(getApplication())
            val config = preset.config
            val mode = try { EmojiRenderingMode.valueOf(config?.renderingMode ?: "FULL") } catch(e: Exception) { EmojiRenderingMode.FULL }
            
            val wallpaperFileInPreset = File(preset.wallpaperPath)
            
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

            val hasImage = wallpaperFileInPreset.exists()
            val color = config?.wallpaperColor ?: 0xFF2196F3.toInt()
            val liteBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            
            when (mode) {
                EmojiRenderingMode.LITE -> {
                    wm.setBitmap(liteBitmap)
                    FileOutputStream(wallpaperFile).use { liteBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                EmojiRenderingMode.BALANCE -> {
                    if (hasImage) {
                        val fullBitmap = BitmapFactory.decodeFile(preset.wallpaperPath)
                        wm.setBitmap(fullBitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                    wm.setBitmap(liteBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    FileOutputStream(wallpaperFile).use { liteBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                EmojiRenderingMode.FULL -> {
                    if (hasImage) {
                        val fullBitmap = BitmapFactory.decodeFile(preset.wallpaperPath)
                        wm.setBitmap(fullBitmap)
                        FileOutputStream(wallpaperFile).use { fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
            }

            _currentWallpaperPresetName.value = preset.name
            prefs.edit().putString("current_wallpaper_preset", preset.name).apply()
            updateBlurredWallpaper()
            notifyWallpaperChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isApplyingWallpaper.value = false
        }
    }
}

fun MainViewModel.addNewWallpaperPreset(croppedBitmap: Bitmap, name: String, originalBitmap: Bitmap? = null) {
    viewModelScope.launch(Dispatchers.IO) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val wallpaperDir = File(documentsDir, "Iteration/Wallpaper/$name")
        if (!wallpaperDir.exists()) wallpaperDir.mkdirs()

        val wallpaperFile = File(wallpaperDir, "wallpaper.png")
        val previewFile = File(wallpaperDir, "preview.jpg")
        val configFile = File(wallpaperDir, "config.json")
        val originalFile = File(wallpaperDir, "original.png")

        FileOutputStream(wallpaperFile).use { croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        originalBitmap?.let { 
            FileOutputStream(originalFile).use { out -> it.compress(Bitmap.CompressFormat.PNG, 100, out) }
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
        val dm = getApplication<android.app.Application>().resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        
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
    }
}
