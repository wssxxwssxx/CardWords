package com.example.cardwords.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DictionaryApiResponse(
    val word: String = "",
    val phonetic: String? = null,
    val phonetics: List<PhoneticEntry> = emptyList(),
    val meanings: List<MeaningEntry> = emptyList(),
)

@Serializable
data class PhoneticEntry(
    val text: String? = null,
    val audio: String? = null,
)

@Serializable
data class MeaningEntry(
    val partOfSpeech: String = "",
    val definitions: List<DefinitionEntry> = emptyList(),
)

@Serializable
data class DefinitionEntry(
    val definition: String = "",
    val example: String? = null,
)
