package com.example.cardwords.data.model

data class WordPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val description: String,
    val level: PackLevel,
    val words: List<PackWord>,
) {
    val sourceTag: String get() = "pack_$id"
    val wordCount: Int get() = words.size
}

enum class PackLevel(val label: String) {
    BY_LEVEL("По уровню"),
    BY_TOPIC("По теме"),
    SPECIAL("Особые"),
}

data class PackWord(
    val original: String,
    val translation: String,
    val transcription: String,
    val category: String = "",
)
