package com.example.cardwords.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Word(
    val id: Long,
    val original: String,
    val translation: String,
    val transcription: String = "",
    val category: String = "",
    val isInDictionary: Boolean = false,
    val addedAt: Long? = null,
    val source: String = "hardcoded",
)
