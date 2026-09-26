package com.liferlighdow.iteration.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import java.io.File

@Composable
fun VideoWallpaperPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val mediaPlayer = remember(videoPath) {
        MediaPlayer().apply {
            val file = File(videoPath)
            if (file.exists()) {
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(0f, 0f)
                prepareAsync()
            } else {
                Log.e("VideoWallpaperPlayer", "Video file does not exist: $videoPath")
            }
        }
    }

    DisposableEffect(lifecycleOwner, mediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (!mediaPlayer.isPlaying) mediaPlayer.start()
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val listener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayer.setSurface(Surface(surfaceTexture))
                            mediaPlayer.setOnPreparedListener { mp ->
                                mp.start()
                            }
                            if (!mediaPlayer.isPlaying) {
                                mediaPlayer.start()
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer.setSurface(null)
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                }

                surfaceTextureListener = listener

                if (isAvailable && surfaceTexture != null) {
                    try {
                        mediaPlayer.setSurface(Surface(surfaceTexture))
                        mediaPlayer.setOnPreparedListener { mp -> mp.start() }
                        if (!mediaPlayer.isPlaying) {
                            mediaPlayer.start()
                        }
                    } catch (_: Exception) {}
                }
            }
        },
        update = { textureView ->
            if (textureView.isAvailable && textureView.surfaceTexture != null) {
                try {
                    mediaPlayer.setSurface(Surface(textureView.surfaceTexture))
                    if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                    }
                } catch (_: Exception) {}
            }
        },
        modifier = modifier
    )
}

@Composable
fun GifWallpaperPlayer(
    gifPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(File(gifPath))
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
