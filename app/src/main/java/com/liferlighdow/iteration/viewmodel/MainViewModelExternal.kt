package com.liferlighdow.iteration.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.data.CalendarEventModel
import com.liferlighdow.iteration.data.ContactModel
import com.liferlighdow.iteration.data.FileModel
import com.liferlighdow.iteration.data.WeatherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.checkSystemNetworkStatus() {
    val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork
    val caps = cm.getNetworkCapabilities(activeNetwork)
    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    _isSystemNetworkEnabled.value = hasInternet
}

fun MainViewModel.fetchWeather() {
    viewModelScope.launch {
        weatherRepository.fetchWeather(_isNetworkAccessEnabled.value)
    }
}

fun MainViewModel.setWeatherProvider(provider: WeatherProvider) {
    weatherRepository.setWeatherProvider(provider)
    fetchWeather()
}

fun MainViewModel.updateLocation(lat: Double, lon: Double, name: String) {
    weatherRepository.updateLocation(lat, lon, name)
    fetchWeather()
}

fun MainViewModel.resetToIpLocation() {
    weatherRepository.resetToIpLocation()
    fetchWeather()
}

fun MainViewModel.fetchExchangeRates() {
    viewModelScope.launch {
        currencyRepository.fetchExchangeRates(_isNetworkAccessEnabled.value)
    }
}

fun MainViewModel.loadContacts() {
    viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@launch
        }

        val contactList = mutableListOf<ContactModel>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx)
                val number = cursor.getString(numIdx)
                val photoUri = cursor.getString(photoIdx)

                var photoBitmap: Bitmap? = null
                if (photoUri != null) {
                    try {
                        val uri = Uri.parse(photoUri)
                        photoBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                            android.graphics.ImageDecoder.decodeBitmap(source)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                    } catch (e: Exception) {}
                }
                contactList.add(ContactModel(id, name, number, photoBitmap))
            }
        }
        _contacts.value = contactList.distinctBy { it.phoneNumber }
    }
}

fun MainViewModel.loadCalendarEvents() {
    viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return@launch
        }

        val eventList = mutableListOf<CalendarEventModel>()
        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )
        
        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs = arrayOf(System.currentTimeMillis().toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
            val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
            val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
            val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            val calIdx = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                eventList.add(CalendarEventModel(
                    id = cursor.getLong(idIdx),
                    title = cursor.getString(titleIdx) ?: "No Title",
                    startTime = cursor.getLong(startIdx),
                    endTime = cursor.getLong(endIdx),
                    location = cursor.getString(locIdx),
                    calendarName = cursor.getString(calIdx)
                ))
                if (eventList.size >= 50) break
            }
        }
        _calendarEvents.value = eventList
    }
}

fun MainViewModel.loadFiles() {
    viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val hasBasicPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // We assume media permissions are enough for MediaStore
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        
        val hasAllFilesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else hasBasicPermission

        if (!hasBasicPermission && !hasAllFilesPermission) return@launch

        val fileList = mutableListOf<FileModel>()
        
        // 1. Use MediaStore - works even with Scoped Storage for many items
        try {
            val externalUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            
            context.contentResolver.query(
                externalUri, 
                projection, 
                null, null, 
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                
                while (cursor.moveToNext() && fileList.size < 2000) {
                    val path = cursor.getString(dataIdx) ?: continue
                    val file = File(path)
                    fileList.add(FileModel(
                        name = cursor.getString(nameIdx) ?: file.name,
                        path = path,
                        isDirectory = file.isDirectory,
                        size = cursor.getLong(sizeIdx),
                        lastModified = cursor.getLong(dateIdx) * 1000
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Manual scan - only if we have full access
        if (hasAllFilesPermission) {
            val root = Environment.getExternalStorageDirectory()
            val commonDirs = listOf(
                root, 
                File(root, "Download"), 
                File(root, "Documents"), 
                File(root, "Pictures"), 
                File(root, "Music"),
                File(root, "Movies"),
                File(root, "DCIM")
            )
            
            for (dir in commonDirs) {
                if (!dir.exists()) continue
                dir.listFiles()?.forEach { file ->
                    if (!file.name.startsWith(".")) {
                        fileList.add(FileModel(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            size = file.length(),
                            lastModified = file.lastModified()
                        ))
                    }
                }
            }
        }
        
        _files.value = fileList.distinctBy { it.path }.sortedByDescending { it.lastModified }
    }
}
