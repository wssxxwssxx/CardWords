package com.example.cardwords.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cardwords.navigation.AchievementsRoute
import com.example.cardwords.navigation.HomeRoute
import com.example.cardwords.navigation.MixedStudyRoute
import com.example.cardwords.navigation.ProfileTabRoute
import com.example.cardwords.navigation.StatsTabRoute
import com.example.cardwords.navigation.StudyModeSelectionRoute
import com.example.cardwords.navigation.DungeonRoute
import com.example.cardwords.navigation.WordFallRoute
import com.example.cardwords.navigation.WordPacksRoute
import com.example.cardwords.navigation.WordSelectionRoute
import com.example.cardwords.navigation.WordsTabRoute
import com.example.cardwords.ui.dictionary.DictionaryScreen
import com.example.cardwords.ui.home.HomeScreen
import com.example.cardwords.ui.profile.ProfileScreen
import com.example.cardwords.ui.stats.StatsScreen
import com.example.cardwords.ui.theme.SkyBlue
import com.example.cardwords.ui.theme.Surface0
import com.example.cardwords.ui.theme.Surface1
import com.example.cardwords.ui.theme.Surface2
import com.example.cardwords.ui.theme.TextDim

private data class TabItem(
    val emoji: String,
    val label: String,
    val route: Any,
)

private val tabs = listOf(
    TabItem("\uD83C\uDFE0", "Главная", HomeRoute),
    TabItem("\uD83D\uDCD6", "Слова", WordsTabRoute),
    TabItem("\uD83D\uDCCA", "Статистика", StatsTabRoute),
    TabItem("\uD83D\uDC64", "Профиль", ProfileTabRoute),
)

@Composable
fun MainScreen(
    outerNavController: NavHostController,
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()

    Scaffold(
        containerColor = Surface0,
        bottomBar = {
            // Floating pill nav bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp, 0.dp, 12.dp, 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface1)
                        .border(1.dp, Surface2, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { tab ->
                        val selected = navBackStackEntry?.destination?.hasRoute(tab.route::class) == true

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (selected) {
                                        Modifier.background(SkyBlue.copy(alpha = 0.07f))
                                    } else Modifier,
                                )
                                .clickable {
                                    tabNavController.navigate(tab.route) {
                                        popUpTo(tabNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = tab.emoji,
                                fontSize = 20.sp,
                            )

                            Text(
                                text = tab.label,
                                fontSize = 9.sp,
                                color = if (selected) SkyBlue else TextDim,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            )

                            // Active pip
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .width(18.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SkyBlue),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = tabNavController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNavigateToStudy = {
                        outerNavController.navigate(StudyModeSelectionRoute)
                    },
                    onNavigateToSmartSession = {
                        outerNavController.navigate(
                            MixedStudyRoute(
                                multipleChoice = true,
                                flashcard = true,
                                typing = true,
                                letterAssembly = true,
                                isSmartSession = true,
                            )
                        )
                    },
                    onNavigateToWordFall = {
                        outerNavController.navigate(WordFallRoute)
                    },
                    onNavigateToDungeon = {
                        outerNavController.navigate(DungeonRoute)
                    },
                )
            }

            composable<WordsTabRoute> {
                DictionaryScreen(
                    onNavigateToAddWords = {
                        outerNavController.navigate(WordPacksRoute)
                    },
                    onNavigateBack = {},
                    onNavigateToWordSearch = {
                        outerNavController.navigate(WordSelectionRoute)
                    },
                    showTopBar = false,
                )
            }

            composable<StatsTabRoute> {
                StatsScreen(
                    onNavigateBack = {},
                    onNavigateToAchievements = {
                        outerNavController.navigate(AchievementsRoute)
                    },
                    showTopBar = false,
                )
            }

            composable<ProfileTabRoute> {
                ProfileScreen()
            }
        }
    }
}
