package com.example.cardwords.data.model

import com.example.cardwords.data.local.DatabaseRepository

object OnboardingManager {
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_HOME_COACH_SHOWN = "home_coach_shown"
    private const val KEY_DICTIONARY_COACH_SHOWN = "dictionary_coach_shown"

    fun isOnboardingCompleted(repository: DatabaseRepository): Boolean =
        false // TODO: restore: repository.getSetting(KEY_ONBOARDING_COMPLETED) == "true"

    fun setOnboardingCompleted(repository: DatabaseRepository) {
        repository.setSetting(KEY_ONBOARDING_COMPLETED, "true")
    }

    fun isHomeCoachShown(repository: DatabaseRepository): Boolean =
        repository.getSetting(KEY_HOME_COACH_SHOWN) == "true"

    fun setHomeCoachShown(repository: DatabaseRepository) {
        repository.setSetting(KEY_HOME_COACH_SHOWN, "true")
    }

    fun isDictionaryCoachShown(repository: DatabaseRepository): Boolean =
        repository.getSetting(KEY_DICTIONARY_COACH_SHOWN) == "true"

    fun setDictionaryCoachShown(repository: DatabaseRepository) {
        repository.setSetting(KEY_DICTIONARY_COACH_SHOWN, "true")
    }
}
