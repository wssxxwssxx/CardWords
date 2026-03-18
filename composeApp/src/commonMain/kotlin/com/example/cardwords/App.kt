package com.example.cardwords

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.cardwords.data.model.OnboardingManager
import com.example.cardwords.di.AppModule
import com.example.cardwords.navigation.AchievementsRoute
import com.example.cardwords.navigation.MainRoute
import com.example.cardwords.navigation.MixedStudyRoute
import com.example.cardwords.navigation.OnboardingRoute
import com.example.cardwords.navigation.StudyModeSelectionRoute
import com.example.cardwords.navigation.StudyWordSettingsRoute
import com.example.cardwords.navigation.PackDetailRoute
import com.example.cardwords.navigation.WelcomeRoute
import com.example.cardwords.navigation.DungeonRoute
import com.example.cardwords.navigation.WordFallRoute
import com.example.cardwords.navigation.WordPacksRoute
import com.example.cardwords.navigation.WordSelectionRoute
import com.example.cardwords.ui.WelcomeScreen
import com.example.cardwords.ui.onboarding.OnboardingScreen
import com.example.cardwords.ui.addwords.WordSelectionScreen
import com.example.cardwords.ui.main.MainScreen
import com.example.cardwords.ui.study.StudyMode
import com.example.cardwords.ui.study.StudyModeSelectionScreen
import com.example.cardwords.ui.study.StudyWordSettingsScreen
import com.example.cardwords.ui.study.mixed.MixedStudyScreen
import com.example.cardwords.ui.packs.PackDetailScreen
import com.example.cardwords.ui.packs.WordPacksScreen
import com.example.cardwords.ui.achievements.AchievementsScreen
import com.example.cardwords.ui.game.dungeon.DungeonScreen
import com.example.cardwords.ui.game.wordfall.WordFallScreen
import com.example.cardwords.ui.theme.CardWordsTheme

@Composable
fun App() {
    CardWordsTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = WelcomeRoute,
        ) {
            composable<WelcomeRoute> {
                val repository = AppModule.databaseRepository
                WelcomeScreen(
                    onStartClick = {
                        if (OnboardingManager.isOnboardingCompleted(repository)) {
                            navController.navigate(MainRoute) {
                                popUpTo<WelcomeRoute> { inclusive = true }
                            }
                        } else {
                            navController.navigate(OnboardingRoute) {
                                popUpTo<WelcomeRoute> { inclusive = true }
                            }
                        }
                    },
                    onLoginClick = { /* TODO: navigate to login screen */ },
                )
            }

            composable<OnboardingRoute> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(MainRoute) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    },
                )
            }

            composable<MainRoute> {
                MainScreen(outerNavController = navController)
            }

            composable<StudyModeSelectionRoute> {
                StudyModeSelectionScreen(
                    onNext = { selectedModes ->
                        navController.navigate(
                            StudyWordSettingsRoute(
                                multipleChoice = StudyMode.MULTIPLE_CHOICE in selectedModes,
                                flashcard = StudyMode.FLASHCARD in selectedModes,
                                typing = StudyMode.TYPING in selectedModes,
                                letterAssembly = StudyMode.LETTER_ASSEMBLY in selectedModes,
                            )
                        )
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<StudyWordSettingsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<StudyWordSettingsRoute>()
                StudyWordSettingsScreen(
                    multipleChoice = route.multipleChoice,
                    flashcard = route.flashcard,
                    typing = route.typing,
                    letterAssembly = route.letterAssembly,
                    onStartStudy = { wordCount, wordSource, wordIds ->
                        navController.navigate(
                            MixedStudyRoute(
                                multipleChoice = route.multipleChoice,
                                flashcard = route.flashcard,
                                typing = route.typing,
                                letterAssembly = route.letterAssembly,
                                wordCount = wordCount,
                                wordSource = wordSource,
                                wordIds = wordIds,
                            )
                        )
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<MixedStudyRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MixedStudyRoute>()
                MixedStudyScreen(
                    multipleChoice = route.multipleChoice,
                    flashcard = route.flashcard,
                    typing = route.typing,
                    letterAssembly = route.letterAssembly,
                    wordCount = route.wordCount,
                    wordSource = route.wordSource,
                    wordIds = route.wordIds,
                    isSmartSession = route.isSmartSession,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddWords = {
                        navController.navigate(WordPacksRoute)
                    },
                )
            }

            composable<WordPacksRoute> {
                WordPacksScreen(
                    onNavigateToPackDetail = { packId ->
                        navController.navigate(PackDetailRoute(packId))
                    },
                    onNavigateToWordSearch = {
                        navController.navigate(WordSelectionRoute)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<PackDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<PackDetailRoute>()
                PackDetailScreen(
                    packId = route.packId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<AchievementsRoute> {
                AchievementsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<WordFallRoute> {
                WordFallScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<DungeonRoute> {
                DungeonScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<WordSelectionRoute> {
                WordSelectionScreen(
                    onWordsAdded = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
