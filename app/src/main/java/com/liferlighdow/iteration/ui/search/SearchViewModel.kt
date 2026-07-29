package com.liferlighdow.iteration.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _translationResult = MutableStateFlow<String?>(null)
    val translationResult: StateFlow<String?> = _translationResult.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _webSuggestions = MutableStateFlow<List<String>>(emptyList())
    val webSuggestions: StateFlow<List<String>> = _webSuggestions.asStateFlow()

    private var searchJob: Job? = null
    private var translationJob: Job? = null

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        handleQueryChanges(newQuery)
    }

    private fun handleQueryChanges(q: String) {
        val trimmed = q.trim()
        
        // 1. 處理翻譯邏輯
        translationJob?.cancel()
        if (trimmed.startsWith("tr ", ignoreCase = true)) {
            translationJob = viewModelScope.launch {
                delay(500)
                performTranslation(trimmed)
            }
        } else {
            _translationResult.value = null
        }

        // 2. 處理搜尋建議邏輯
        searchJob?.cancel()
        if (trimmed.length >= 2 && !trimmed.startsWith("tr ", ignoreCase = true) && 
            !trimmed.all { it.isDigit() || "+-*/^%()".contains(it) }) {
            searchJob = viewModelScope.launch {
                delay(300)
                fetchWebSuggestions(trimmed)
            }
        } else {
            _webSuggestions.value = emptyList()
        }
    }

    private suspend fun performTranslation(rawQuery: String) {
        val rawContent = rawQuery.substring(3).trim()
        if (rawContent.isBlank()) return

        val textToTranslate: String
        val targetLang: String

        val pattern = Regex(
            """^(?:["'](.+?)["']|(.+?))\s+to\s+(?:["']([a-zA-Z-]+)["']|([a-zA-Z-]+))$""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(rawContent)

        if (match != null) {
            textToTranslate = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
            targetLang = match.groupValues[3].takeIf { it.isNotEmpty() } ?: match.groupValues[4]
        } else {
            textToTranslate = rawContent.removeSurrounding("\"").removeSurrounding("'")
            targetLang = Locale.getDefault().toLanguageTag()
        }

        _isTranslating.value = true
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=${URLEncoder.encode(textToTranslate, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(res)
                    val resultParts = jsonArray.getJSONArray(0)
                    val translatedText = StringBuilder()
                    for (i in 0 until resultParts.length()) {
                        val part = resultParts.getJSONArray(i).optString(0, "")
                        if (part != "null") translatedText.append(part)
                    }
                    _translationResult.value = translatedText.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTranslating.value = false
            }
        }
    }

    private suspend fun fetchWebSuggestions(q: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=${URLEncoder.encode(q, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(res)
                    val suggestions = jsonArray.getJSONArray(1)
                    val list = mutableListOf<String>()
                    for (i in 0 until minOf(suggestions.length(), 4)) {
                        list.add(suggestions.getString(i))
                    }
                    _webSuggestions.value = list
                }
            } catch (e: Exception) {
                _webSuggestions.value = emptyList()
            }
        }
    }

    fun clear() {
        _query.value = ""
        _translationResult.value = null
        _webSuggestions.value = emptyList()
    }
}
