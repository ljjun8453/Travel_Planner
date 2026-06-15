package com.example.momentrip

data class TravelPlan(
    val no: Int = 0,
    val place: String,
    val planDate: String,
    val memo: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)
