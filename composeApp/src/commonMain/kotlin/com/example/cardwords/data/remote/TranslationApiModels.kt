package com.example.cardwords.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TranslationApiResponse(
    val responseData: TranslationResponseData = TranslationResponseData(),
    val responseStatus: Int = 0,
)

@Serializable
data class TranslationResponseData(
    val translatedText: String = "",
    val match: Double? = null,
)
