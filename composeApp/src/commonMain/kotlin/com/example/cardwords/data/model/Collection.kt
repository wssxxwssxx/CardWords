package com.example.cardwords.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Collection(
    val id: String,
    val name: String,
    val description: String?,
    val cardsCount: Int,
)

@Immutable
data class CollectionCard(
    val id: String,
    val wordOriginal: String,
    val wordTranslation: String,
)

@Immutable
data class CollectionDetail(
    val collection: Collection,
    val cards: List<CollectionCard>,
)

@Immutable
data class StudentSummary(
    val id: String,
    val email: String,
    val name: String,
    val status: StudentStatus,
)

@Immutable
data class TeacherSummary(
    val id: String,
    val email: String,
    val name: String,
)

@Immutable
enum class StudentStatus { PENDING, ACTIVE;

    companion object {
        fun fromWire(value: String?): StudentStatus = when (value?.lowercase()) {
            "active" -> ACTIVE
            else -> PENDING // safe default for unknown / missing
        }
    }
}

@Immutable
enum class AssignmentStatus { ASSIGNED, ADDED;

    companion object {
        fun fromWire(value: String?): AssignmentStatus = when (value?.lowercase()) {
            "added" -> ADDED
            else -> ASSIGNED // default for unknown → safe
        }
    }
}

@Immutable
data class AssignedCollection(
    val collection: Collection,
    val teacherName: String,
    val status: AssignmentStatus,
    /** Computed on the client by comparing cardsCount with locally-stored last-known count. */
    val hasNewCards: Boolean,
)
