package com.liferlighdow.iteration.viewmodel

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateRemoteModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private var currentTranslator: Translator? = null
private var currentModelCode: String? = null

// 管理已下載模型
private val _downloadedModels = MutableStateFlow<Set<String>>(emptySet())
val MainViewModel.downloadedModels get() = _downloadedModels.asStateFlow()

// 管理下載進度 (0.0 - 1.0)
private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
val MainViewModel.downloadProgress get() = _downloadProgress.asStateFlow()

// 管理刪除中狀態
private val _deletingModels = MutableStateFlow<Set<String>>(emptySet())
val MainViewModel.deletingModels get() = _deletingModels.asStateFlow()

// 任務追蹤
private val activeDownloadJobs = mutableMapOf<String, Job>()

fun MainViewModel.refreshDownloadedModels() {
    viewModelScope.launch {
        val modelManager = RemoteModelManager.getInstance()
        val downloaded = TranslateLanguage.getAllLanguages().filter { lang ->
            val model = TranslateRemoteModel.Builder(lang).build()
            modelManager.isModelDownloaded(model).await()
        }.toSet()
        _downloadedModels.value = downloaded
    }
}

fun MainViewModel.downloadModel(lang: String) {
    if (_downloadProgress.value.containsKey(lang)) return
    
    // 強制檢查網路開關
    if (!isNetworkAccessEnabled.value) return

    val job = viewModelScope.launch {
        // 1. 初始化進度並觸發 UI 顯示
        _downloadProgress.value = _downloadProgress.value + (lang to 0.05f)
        
        // 2. 啟動模擬進度協程
        val progressJob = launch {
            var p = 0.05f
            while (p < 0.92f) {
                delay(400)
                p += 0.03f
                _downloadProgress.value = _downloadProgress.value + (lang to p)
            }
        }

        try {
            val modelManager = RemoteModelManager.getInstance()
            val model = TranslateRemoteModel.Builder(lang).build()
            // 這裡設定 requireWifi()，這會讓系統盡量將檔案存放至 files 而非 cache
            val conditions = DownloadConditions.Builder().requireWifi().build()
            
            modelManager.download(model, conditions).await()
            
            // 下載成功，填滿 100% 並停留一下
            _downloadProgress.value = _downloadProgress.value + (lang to 1.0f)
            delay(800)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            progressJob.cancel()
            _downloadProgress.value = _downloadProgress.value - lang
            activeDownloadJobs.remove(lang)
            refreshDownloadedModels()
        }
    }
    activeDownloadJobs[lang] = job
}

/**
 * 刪除模型方法 (修復 Unresolved Reference)
 */
fun MainViewModel.deleteModel(lang: String) {
    if (_deletingModels.value.contains(lang)) return
    
    viewModelScope.launch {
        _deletingModels.value += lang
        try {
            val modelManager = RemoteModelManager.getInstance()
            val model = TranslateRemoteModel.Builder(lang).build()
            modelManager.deleteDownloadedModel(model).await()
            // 模擬刪除延遲以顯示動畫
            delay(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _deletingModels.value -= lang
            refreshDownloadedModels()
        }
    }
}

fun MainViewModel.cancelDownload(lang: String) {
    activeDownloadJobs[lang]?.cancel()
    activeDownloadJobs.remove(lang)
    _downloadProgress.value = _downloadProgress.value - lang
}

suspend fun MainViewModel.translateOffline(text: String, targetTag: String): String? {
    val targetLang = TranslateLanguage.fromLanguageTag(targetTag) 
        ?: TranslateLanguage.fromLanguageTag(Locale.getDefault().language) 
        ?: TranslateLanguage.CHINESE

    val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH) 
        .setTargetLanguage(targetLang)
        .build()
    
    val modelCode = "en_to_$targetLang"
    
    if (currentModelCode != modelCode) {
        currentTranslator?.close()
        currentTranslator = Translation.getClient(options)
        currentModelCode = modelCode
    }
    
    val translator = currentTranslator ?: return null
    
    return try {
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions).await()
        translator.translate(text).await()
    } catch (e: Exception) {
        null
    }
}
