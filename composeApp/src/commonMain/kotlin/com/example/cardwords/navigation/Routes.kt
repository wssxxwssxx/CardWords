package com.example.cardwords.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data object DictionaryRoute

@Serializable
data object StudyModeSelectionRoute

@Serializable
data class StudyWordSettingsRoute(
    val multipleChoice: Boolean = true,
    val flashcard: Boolean = true,
    val typing: Boolean = true,
    val letterAssembly: Boolean = true,
)

@Serializable
data class MixedStudyRoute(
    val multipleChoice: Boolean = true,
    val flashcard: Boolean = true,
    val typing: Boolean = true,
    val letterAssembly: Boolean = true,
    val wordCount: Int = 0,
    val wordSource: String = "",
    val wordIds: String = "",
    val isSmartSession: Boolean = false,
)

@Serializable
data object WordSelectionRoute

@Serializable
data object WordPacksRoute

@Serializable
data class PackDetailRoute(val packId: String)

@Serializable
data object StatsRoute

@Serializable
data object AchievementsRoute

@Serializable
data object WordFallRoute

@Serializable
data object DungeonRoute

@Serializable
data object MainRoute

@Serializable
data object WordsTabRoute

@Serializable
data object StatsTabRoute

@Serializable
data object ProfileTabRoute
