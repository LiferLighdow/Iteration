package com.liferlighdow.iteration.data

import kotlinx.serialization.Serializable

@Serializable
data class WallpaperConfig(
    val blur: Float? = null,
    val refractionHeight: Float? = null,
    val refractionAmount: Float? = null,
    val chromaticAberration: Boolean? = null,
    val themeMode: String? = null,
    val isMaterialYou: Boolean? = null,
    val wallpaperColor: Int? = null,
    val emojiText: String? = null,
    val emojiPatternStyle: String? = null,
    val renderingMode: String? = null
)

enum class EmojiRenderingMode {
    LITE, BALANCE, FULL
}

enum class EmojiPatternStyle {
    SMALL_GRID, MEDIUM_GRID, LARGE_GRID, RINGS, SPIRAL
}

data class WallpaperPreset(
    val name: String,
    val folderPath: String,
    val previewPath: String?,
    val wallpaperPath: String,
    val originalPath: String?,
    val config: WallpaperConfig?
)
