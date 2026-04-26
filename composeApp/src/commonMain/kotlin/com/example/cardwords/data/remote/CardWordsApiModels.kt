package com.example.cardwords.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// Auth
// ═══════════════════════════════════════════════════════════════

@Serializable
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("subscription_status") val subscriptionStatus: String,
)

// ═══════════════════════════════════════════════════════════════
// Cards
// ═══════════════════════════════════════════════════════════════

@Serializable
data class CreateCardRequest(
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String? = null,
)

@Serializable
data class CardResponse(
    val id: String,
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String,
    val status: String, // "unknown", "waiting", "learned"
    @SerialName("correct_streak") val correctStreak: Int,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CheckWordResponse(
    val exists: Boolean,
    @SerialName("card_id") val cardId: String? = null,
)

// ═══════════════════════════════════════════════════════════════
// Tests
// ═══════════════════════════════════════════════════════════════

@Serializable
data class TestSessionResponse(
    @SerialName("test_session_id") val testSessionId: String,
    val questions: List<TestQuestion>,
)

@Serializable
data class TestQuestion(
    val id: String,
    val type: String,
    val sentence: String,
    val options: List<String>,
    @SerialName("correct_answer") val correctAnswer: String,
    @SerialName("translation_hint") val translationHint: String,
)

@Serializable
data class SubmitTestRequest(
    val answers: List<SubmitAnswer>,
)

@Serializable
data class SubmitAnswer(
    @SerialName("question_id") val questionId: String,
    val answer: String,
)

@Serializable
data class TestResultResponse(
    val score: Int,
    val total: Int,
    @SerialName("correct_percentage") val correctPercentage: Double,
    @SerialName("updated_cards") val updatedCards: List<UpdatedCard>,
)

@Serializable
data class UpdatedCard(
    @SerialName("card_id") val cardId: String,
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String,
    val status: String,
    @SerialName("correct_streak") val correctStreak: Int,
    @SerialName("is_correct") val isCorrect: Boolean,
)

// ═══════════════════════════════════════════════════════════════
// Stats
// ═══════════════════════════════════════════════════════════════

@Serializable
data class StatsResponse(
    @SerialName("total_cards") val totalCards: Int,
    @SerialName("unknown_count") val unknownCount: Int,
    @SerialName("waiting_count") val waitingCount: Int,
    @SerialName("learned_count") val learnedCount: Int,
    val streak: Int,
    @SerialName("total_tests") val totalTests: Int,
    @SerialName("avg_score") val avgScore: Double,
    @SerialName("last_7_days") val last7Days: List<DayStats>,
)

@Serializable
data class DayStats(
    val date: String,
    @SerialName("cards_added") val cardsAdded: Int,
    @SerialName("tests_completed") val testsCompleted: Int,
)

// ═══════════════════════════════════════════════════════════════
// Health
// ═══════════════════════════════════════════════════════════════

@Serializable
data class HealthResponse(
    val status: String,
    val db: String,
)

// ═══════════════════════════════════════════════════════════════
// Role
// ═══════════════════════════════════════════════════════════════

@Serializable
data class SwitchRoleRequest(val role: String)

// ═══════════════════════════════════════════════════════════════
// Teacher / Student summaries
// ═══════════════════════════════════════════════════════════════

@Serializable
data class StudentSummaryDto(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("cards_count") val cardsCount: Int = 0,
)

@Serializable
data class TeacherSummaryDto(
    val id: String,
    val email: String,
    val name: String,
)

@Serializable
data class InviteStudentRequest(val email: String)

// ═══════════════════════════════════════════════════════════════
// Collections (teacher)
// ═══════════════════════════════════════════════════════════════

@Serializable
data class CollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
)

@Serializable
data class CollectionDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
    val cards: List<CollectionCardDto> = emptyList(),
)

@Serializable
data class CollectionCardDto(
    val id: String,
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String,
)

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val description: String,
)

@Serializable
data class UpdateCollectionRequest(
    val name: String,
    val description: String,
)

@Serializable
data class AddCollectionCardRequest(
    @SerialName("word_original") val wordOriginal: String,
    @SerialName("word_translation") val wordTranslation: String? = null,
)

// ═══════════════════════════════════════════════════════════════
// Assigned collections (student side)
// ═══════════════════════════════════════════════════════════════

@Serializable
data class AssignedCollectionDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("cards_count") val cardsCount: Int = 0,
    @SerialName("teacher_name") val teacherName: String = "",
    val status: String = "assigned",
)
