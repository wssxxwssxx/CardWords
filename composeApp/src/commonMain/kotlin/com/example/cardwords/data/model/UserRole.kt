package com.example.cardwords.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class UserRole {
    STUDENT, TEACHER;

    val wireValue: String get() = name.lowercase() // server uses "student"/"teacher"

    companion object {
        fun fromWire(value: String?): UserRole? = when (value?.lowercase()) {
            "student" -> STUDENT
            "teacher" -> TEACHER
            else -> null
        }
    }
}
