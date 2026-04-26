package com.example.cardwords.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cardwords.data.model.UserRole
import com.example.cardwords.di.AppModule
import com.example.cardwords.navigation.AchievementsRoute
import com.example.cardwords.navigation.HomeRoute
import com.example.cardwords.navigation.MixedStudyRoute
import com.example.cardwords.navigation.MyTeachersTabRoute
import com.example.cardwords.navigation.ProfileTabRoute
import com.example.cardwords.navigation.StatsTabRoute
import com.example.cardwords.navigation.StudyModeSelectionRoute
import com.example.cardwords.navigation.DungeonRoute
import com.example.cardwords.navigation.TeachingTabRoute
import com.example.cardwords.navigation.WordFallRoute
import com.example.cardwords.navigation.WordPacksRoute
import com.example.cardwords.navigation.WordSelectionRoute
import com.example.cardwords.navigation.WordsTabRoute
import com.example.cardwords.ui.dictionary.DictionaryScreen
import com.example.cardwords.ui.home.HomeScreen
import com.example.cardwords.ui.profile.ProfileScreen
import com.example.cardwords.ui.stats.StatsScreen
import com.example.cardwords.ui.student.MyTeachersScreen
import com.example.cardwords.ui.theme.LightBg
import com.example.cardwords.ui.theme.LightCard
import com.example.cardwords.ui.theme.LightFg
import com.example.cardwords.ui.theme.LightFgMuted
import com.example.cardwords.ui.theme.LightFgSecondary
import com.example.cardwords.ui.theme.Surface0

// ─────────────────────────────────────────────
//  Nav icon SVG path data
// ─────────────────────────────────────────────
private object NavIconPaths {
    // icon-home.svg (24×24)
    const val HOME =
        "M3 10.5L12 3L21 10.5V20C21 20.2652 20.8946 20.5196 20.7071 20.7071C20.5196 20.8946 " +
        "20.2652 21 20 21H15V15H9V21H4C3.73478 21 3.48043 20.8946 3.29289 20.7071C3.10536 " +
        "20.5196 3 20.2652 3 20V10.5Z"

    // Frame (5).svg — open book (24×24)
    const val BOOK_SPINE =
        "M4 19.5C4 18.837 4.26339 18.2011 4.73223 17.7322C5.20107 17.2634 5.83696 17 6.5 17H20"
    const val BOOK_COVER =
        "M6.5 2H20V22H6.5C5.83696 22 5.20107 21.7366 4.73223 21.2678C4.26339 20.7989 4 20.163 " +
        "4 19.5V4.5C4 3.83696 4.26339 3.20107 4.73223 2.73223C5.20107 2.26339 5.83696 2 6.5 2Z"

    // Frame (6).svg — bar chart (24×24)
    const val BAR_CHART = "M18 20V10M12 20V4M6 20V14"

    // Frame (7).svg — person (24×24)
    const val PERSON_HEAD =
        "M12 12C14.2091 12 16 10.2091 16 8C16 5.79086 14.2091 4 12 4C9.79086 4 8 5.79086 8 8C8 10.2091 9.79086 12 12 12Z"
    const val PERSON_BODY =
        "M5 20C5 16.5 8.1 14 12 14C15.9 14 19 16.5 19 20"
}

// ─────────────────────────────────────────────
//  Nav icon composables
// ─────────────────────────────────────────────
@Composable
private fun HomeNavIcon(modifier: Modifier = Modifier, color: Color) {
    val path = remember { PathParser().parsePathString(NavIconPaths.HOME).toPath() }
    Canvas(modifier) {
        val sx = size.width / 24f; val sy = size.height / 24f
        scale(sx, sy, Offset.Zero) {
            drawPath(path, color, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun BookNavIcon(modifier: Modifier = Modifier, color: Color) {
    val spine = remember { PathParser().parsePathString(NavIconPaths.BOOK_SPINE).toPath() }
    val cover = remember { PathParser().parsePathString(NavIconPaths.BOOK_COVER).toPath() }
    Canvas(modifier) {
        val sx = size.width / 24f; val sy = size.height / 24f
        val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(sx, sy, Offset.Zero) {
            drawPath(cover, color, style = stroke)
            drawPath(spine, color, style = stroke)
        }
    }
}

@Composable
private fun BarChartNavIcon(modifier: Modifier = Modifier, color: Color) {
    val path = remember { PathParser().parsePathString(NavIconPaths.BAR_CHART).toPath() }
    Canvas(modifier) {
        val sx = size.width / 24f; val sy = size.height / 24f
        scale(sx, sy, Offset.Zero) {
            drawPath(path, color, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun PersonNavIcon(modifier: Modifier = Modifier, color: Color) {
    val head = remember { PathParser().parsePathString(NavIconPaths.PERSON_HEAD).toPath() }
    val body = remember { PathParser().parsePathString(NavIconPaths.PERSON_BODY).toPath() }
    Canvas(modifier) {
        val sx = size.width / 24f; val sy = size.height / 24f
        val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(sx, sy, Offset.Zero) {
            drawPath(head, color, style = stroke)
            drawPath(body, color, style = stroke)
        }
    }
}

// ─────────────────────────────────────────────
//  Tab definition
// ─────────────────────────────────────────────
private data class TabItem(
    val label: String,
    val route: Any,
    val icon: @Composable (Modifier, Color) -> Unit,
)

private val studentTabs = listOf(
    TabItem("Главная",    HomeRoute,          { m, c -> HomeNavIcon(m, c) }),
    TabItem("Слова",      WordsTabRoute,      { m, c -> BookNavIcon(m, c) }),
    TabItem("Статистика", StatsTabRoute,      { m, c -> BarChartNavIcon(m, c) }),
    TabItem("Учителя",    MyTeachersTabRoute, { m, c -> PersonNavIcon(m, c) }),
    TabItem("Профиль",    ProfileTabRoute,    { m, c -> PersonNavIcon(m, c) }),
)

private val teacherTabs = listOf(
    TabItem("Главная",      HomeRoute,        { m, c -> HomeNavIcon(m, c) }),
    TabItem("Слова",        WordsTabRoute,    { m, c -> BookNavIcon(m, c) }),
    TabItem("Статистика",   StatsTabRoute,    { m, c -> BarChartNavIcon(m, c) }),
    TabItem("Преподавание", TeachingTabRoute, { m, c -> PersonNavIcon(m, c) }),
    TabItem("Профиль",      ProfileTabRoute,  { m, c -> PersonNavIcon(m, c) }),
)

// ─────────────────────────────────────────────
//  Main screen
// ─────────────────────────────────────────────
@Composable
fun MainScreen(
    outerNavController: NavHostController,
    onLogout: () -> Unit = {},
) {
    val authManager = AppModule.authManager
    val role by authManager.roleFlow.collectAsStateWithLifecycle()
    val tabs = remember(role) {
        when (role) {
            UserRole.STUDENT -> studentTabs
            UserRole.TEACHER -> teacherTabs
            null             -> emptyList()
        }
    }

    key(role) {
        val tabNavController = rememberNavController()
        val navBackStackEntry by tabNavController.currentBackStackEntryAsState()

        Scaffold(
            containerColor = LightBg,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightCard)
                        .navigationBarsPadding(),
                ) {
                    // Top divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(LightFgMuted.copy(alpha = 0.3f)),
                    )
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        tabs.forEach { tab ->
                            val selected = navBackStackEntry?.destination
                                ?.hasRoute(tab.route::class) == true
                            val iconColor = if (selected) LightFg else LightFgSecondary

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable {
                                        tabNavController.navigate(tab.route) {
                                            popUpTo(tabNavController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    }
                                    .padding(top = 8.dp),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                                verticalArrangement   = Arrangement.spacedBy(3.dp),
                            ) {
                                tab.icon(Modifier.size(22.dp), iconColor)
                                Text(
                                    text       = tab.label,
                                    fontSize   = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color      = iconColor,
                                )
                            }
                        }
                    }
                }
            },
        ) { paddingValues ->
            NavHost(
                navController    = tabNavController,
                startDestination = HomeRoute,
                modifier         = Modifier.padding(paddingValues),
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
                                    flashcard      = true,
                                    typing         = true,
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
                        onNavigateToTest = {
                            if (authManager.isLoggedIn()) {
                                outerNavController.navigate(com.example.cardwords.navigation.TestRoute)
                            } else {
                                outerNavController.navigate(com.example.cardwords.navigation.AuthRoute)
                            }
                        },
                    )
                }

                composable<WordsTabRoute> {
                    DictionaryScreen(
                        onNavigateToAddWords  = { outerNavController.navigate(WordPacksRoute) },
                        onNavigateBack        = {
                            tabNavController.navigate(HomeRoute) {
                                popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToWordSearch = { outerNavController.navigate(WordSelectionRoute) },
                        showTopBar            = false,
                    )
                }

                composable<StatsTabRoute> {
                    StatsScreen(
                        onNavigateBack          = {},
                        onNavigateToAchievements = { outerNavController.navigate(AchievementsRoute) },
                        showTopBar              = false,
                    )
                }

                composable<MyTeachersTabRoute> {
                    MyTeachersScreen()
                }
                composable<TeachingTabRoute>   { /* TODO Task 16 */ Box(Modifier.fillMaxSize()) }

                composable<ProfileTabRoute> {
                    ProfileScreen(onLogout = onLogout)
                }
            }
        }
    }
}
