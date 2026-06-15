package com.example.momentrip

object TravelPhotoStore {
    private const val SEPARATOR = "\n"

    fun join(uris: List<String>): String? {
        return uris.filter { it.isNotBlank() }.joinToString(SEPARATOR).ifBlank { null }
    }

    fun split(value: String?): List<String> {
        return value.orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun first(value: String?): String? {
        return split(value).firstOrNull()
    }
}
