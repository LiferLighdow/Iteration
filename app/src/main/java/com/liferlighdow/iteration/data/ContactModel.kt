package com.liferlighdow.iteration.data

import android.graphics.Bitmap

data class ContactModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photo: Bitmap? = null
)