package com.example.cardwords.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class FetchedWordResult(
    val original: String,
    val translation: String,
    val transcription: String,
    val definitions: List<String>,
)

class WordApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Detects whether the input contains Cyrillic characters (Russian).
     */
    private fun isCyrillic(text: String): Boolean {
        return text.any { it in '\u0400'..'\u04FF' }
    }

    suspend fun fetchWordWithTranslation(inputWord: String): Result<FetchedWordResult> {
        return try {
            val word = inputWord.trim().lowercase()
            val isRussianInput = isCyrillic(word)

            if (isRussianInput) {
                fetchFromRussian(word)
            } else {
                fetchFromEnglish(word)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * User typed a Russian word → translate to English, then fetch English dictionary data.
     * original = English word, translation = Russian word (input).
     */
    private suspend fun fetchFromRussian(russianWord: String): Result<FetchedWordResult> {
        // Step 1: Translate Russian → English
        var englishTranslation = ""
        try {
            val transResponse: TranslationApiResponse =
                client.get("https://api.mymemory.translated.net/get?q=$russianWord&langpair=ru|en").body()
            englishTranslation = transResponse.responseData.translatedText.lowercase()
        } catch (_: Exception) {
            // Translation API failed
        }

        if (englishTranslation.isBlank() || englishTranslation.lowercase().trim() == russianWord.lowercase().trim()) {
            return Result.failure(Exception("Could not translate the word"))
        }

        // Step 2: Fetch English dictionary data (transcription, definitions)
        var transcription = ""
        var definitions = emptyList<String>()
        try {
            val dictResponse: List<DictionaryApiResponse> =
                client.get("https://api.dictionaryapi.dev/api/v2/entries/en/$englishTranslation").body()
            val entry = dictResponse.firstOrNull()
            transcription = entry?.phonetic
                ?: entry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                ?: ""
            definitions = entry?.meanings?.flatMap { meaning ->
                meaning.definitions.take(2).map { def ->
                    "${meaning.partOfSpeech}: ${def.definition}"
                }
            } ?: emptyList()
        } catch (_: Exception) {
            // Dictionary API failed, continue without transcription
        }

        return Result.success(
            FetchedWordResult(
                original = englishTranslation,
                translation = russianWord,
                transcription = transcription,
                definitions = definitions,
            )
        )
    }

    /**
     * User typed an English word → translate to Russian, fetch English dictionary data.
     * original = English word (input), translation = Russian word.
     */
    private suspend fun fetchFromEnglish(englishWord: String): Result<FetchedWordResult> {
        var transcription = ""
        var definitions = emptyList<String>()
        try {
            val dictResponse: List<DictionaryApiResponse> =
                client.get("https://api.dictionaryapi.dev/api/v2/entries/en/$englishWord").body()
            val entry = dictResponse.firstOrNull()
            transcription = entry?.phonetic
                ?: entry?.phonetics?.firstOrNull { !it.text.isNullOrBlank() }?.text
                ?: ""
            definitions = entry?.meanings?.flatMap { meaning ->
                meaning.definitions.take(2).map { def ->
                    "${meaning.partOfSpeech}: ${def.definition}"
                }
            } ?: emptyList()
        } catch (_: Exception) {
            // Dictionary API failed, continue without transcription
        }

        var translation = ""
        try {
            val transResponse: TranslationApiResponse =
                client.get("https://api.mymemory.translated.net/get?q=$englishWord&langpair=en|ru").body()
            translation = transResponse.responseData.translatedText
        } catch (_: Exception) {
            // Translation API failed
        }

        if (translation.isBlank() || translation.lowercase().trim() == englishWord.lowercase().trim()) {
            return Result.failure(Exception("Could not translate the word"))
        }

        return Result.success(
            FetchedWordResult(
                original = englishWord,
                translation = translation,
                transcription = transcription,
                definitions = definitions,
            )
        )
    }
}
