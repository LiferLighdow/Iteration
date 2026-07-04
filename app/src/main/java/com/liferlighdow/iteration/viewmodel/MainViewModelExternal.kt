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
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.liferlighdow.iteration.data.ContactModel
import com.liferlighdow.iteration.data.WeatherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
