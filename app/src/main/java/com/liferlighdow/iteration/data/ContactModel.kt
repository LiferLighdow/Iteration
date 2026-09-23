package com.liferlighdow.iteration.data

data class ContactModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)