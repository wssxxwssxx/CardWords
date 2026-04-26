package com.example.cardwords.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class CardWordsApiClient(
    private val baseUrl: String = "http://64.188.60.84",
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

    private fun bearerHeader(token: String) = "Bearer $token"

    // ═══════════════════════════════════════════════════════════════
    // Health
    // ═══════════════════════════════════════════════════════════════

    suspend fun healthCheck(): Result<HealthResponse> = runCatching {
        client.get("$baseUrl/health").body()
    }

    // ═══════════════════════════════════════════════════════════════
    // Auth
    // ═══════════════════════════════════════════════════════════════

    suspend fun register(email: String, name: String, password: String): Result<UserResponse> = runCatching {
        client.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, name, password))
        }.body()
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> = runCatching {
        client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()
    }

    suspend fun getMe(token: String): Result<UserResponse> = runCatching {
        client.get("$baseUrl/api/auth/me") {
            header("Authorization", bearerHeader(token))
        }.body()
    }

    suspend fun switchRole(token: String, role: String): Result<Unit> = runCatching {
        val response = client.put("$baseUrl/api/auth/role") {
            header("Authorization", bearerHeader(token))
            contentType(ContentType.Application.Json)
            setBody(SwitchRoleRequest(role))
        }
        if (response.status.value !in 200..299) {
            error("HTTP ${response.status.value}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Cards
    // ═══════════════════════════════════════════════════════════════

    suspend fun getCards(token: String): Result<List<CardResponse>> = runCatching {
        val response = client.get("$baseUrl/api/cards") {
            header("Authorization", bearerHeader(token))
        }
        if (response.status.value !in 200..299) {
            error("HTTP ${response.status.value}")
        }
        response.body<List<CardResponse>>()
    }

    suspend fun createCard(
        token: String,
        wordOriginal: String,
        wordTranslation: String? = null,
    ): Result<CardResponse> = runCatching {
        val response = client.post("$baseUrl/api/cards") {
            header("Authorization", bearerHeader(token))
            contentType(ContentType.Application.Json)
            setBody(CreateCardRequest(wordOriginal, wordTranslation))
        }
        if (response.status.value !in 200..299) {
            error("HTTP ${response.status.value}")
        }
        response.body<CardResponse>()
    }

    suspend fun deleteCard(token: String, cardId: String): Result<Unit> = runCatching {
        val response = client.delete("$baseUrl/api/cards/$cardId") {
            header("Authorization", bearerHeader(token))
        }
        // 404 = already gone on server, treat as success
        if (response.status.value !in 200..299 && response.status.value != 404) {
            error("HTTP ${response.status.value}")
        }
    }

    suspend fun checkWord(token: String, word: String): Result<CheckWordResponse> = runCatching {
        val response = client.get("$baseUrl/api/cards/check") {
            header("Authorization", bearerHeader(token))
            parameter("word", word)
        }
        if (response.status.value !in 200..299) {
            error("HTTP ${response.status.value}")
        }
        response.body<CheckWordResponse>()
    }

    // ═══════════════════════════════════════════════════════════════
    // Tests
    // ═══════════════════════════════════════════════════════════════

    suspend fun startTest(token: String): Result<TestSessionResponse> = runCatching {
        val response = client.post("$baseUrl/api/tests/start") {
            header("Authorization", bearerHeader(token))
            contentType(ContentType.Application.Json)
        }
        if (response.status.value !in 200..299) {
            error("HTTP ${response.status.value}")
        }
        response.body<TestSessionResponse>()
    }

    suspend fun submitTest(
        token: String,
        sessionId: String,
        answers: List<SubmitAnswer>,
    ): Result<TestResultResponse> = runCatching {
        client.post("$baseUrl/api/tests/$sessionId/submit") {
            header("Authorization", bearerHeader(token))
            contentType(ContentType.Application.Json)
            setBody(SubmitTestRequest(answers))
        }.body()
    }

    // ═══════════════════════════════════════════════════════════════
    // Stats
    // ═══════════════════════════════════════════════════════════════

    suspend fun getStats(token: String): Result<StatsResponse> = runCatching {
        client.get("$baseUrl/api/stats") {
            header("Authorization", bearerHeader(token))
        }.body()
    }
}
