package com.example.momentrip

data class TravelRecord(
    val no: Int = 0,
    val place: String = "",
    val visitDate: String = "",
    val memo: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
