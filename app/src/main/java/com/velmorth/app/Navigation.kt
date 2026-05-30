package com.velmorth.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.velmorth.app.domain.repository.UserRepository
import com.velmorth.app.theme.*
import com.velmorth.app.ui.screens.aispeaker.AISpeakerScreen
import com.velmorth.app.ui.screens.home.HomeScreen
import com.velmorth.app.ui.screens.lessons.LessonPathScreen
import com.velmorth.app.ui.screens.lessonplayer.LessonPlayerScreen
import com.velmorth.app.ui.screens.onboarding.OnboardingScreen
import com.velmorth.app.ui.screens.premium.PremiumPaywallScreen
import com.velmorth.app.ui.screens.profile.ProfileScreen
import com.velmorth.app.ui.screens.quiz.QuizScreen
import com.velmorth.app.ui.screens.review.ReviewGardenScreen
import com.velmorth.app.ui.screens.settings.SettingsScreen
import com.velmorth.app.ui.screens.shop.LeafShopScreen
import com.velmorth.app.ui.screens.splash.SplashScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============================================
// SplashViewModel — decides where to go after splash
// =============================================
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    /**
     * Checks if a local user already exists in the DB.
     * If yes → skip onboarding and go to Home.
     * If no  → show onboarding.
     */
    fun determineDestination(
        onNavigateToHome: () -> Unit,
        onNavigateToOnboarding: () -> Unit,
    ) {
        viewModelScope.launch {
            val user = userRepository.getUser().first()
            if (user != null) {
                onNavigateToHome()
            } else {
                onNavigateToOnboarding()
            }
        }
    }
}

// =============================================
// AISpeakerViewModel — reads premium status from DB
// =============================================
@HiltViewModel
class AISpeakerNavViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    val isPremium: StateFlow<Boolean> = userRepository.getUser()
        .map { it?.isPremium ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

// =============================================
// Bottom nav destinations (tabs)
// =============================================
private sealed class BottomNavDest(
    val navKey: NavKey,
    val icon: String,
    val label: String,
) {
    object HomeTab : BottomNavDest(Home, "🏠", "Home")
    object LessonsTab : BottomNavDest(Lessons, "📚", "Lessons")
    object ReviewTab : BottomNavDest(ReviewGarden, "🌸", "Review")
    object ShopTab : BottomNavDest(LeafShop, "🍃", "Shop")
    object ProfileTab : BottomNavDest(Profile, "🌿", "Profile")
}

private val bottomNavItems = listOf(
    BottomNavDest.HomeTab,
    BottomNavDest.LessonsTab,
    BottomNavDest.ReviewTab,
    BottomNavDest.ShopTab,
    BottomNavDest.ProfileTab,
)

// Destinations that show the bottom nav bar
private val mainTabDestinations = setOf(
    Home::class, Lessons::class, ReviewGarden::class, LeafShop::class, Profile::class
)

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Splash)

    // Determine current top-level destination for bottom nav
    val currentDestClass = backStack.lastOrNull()?.let { it::class }
    val showBottomBar = currentDestClass in mainTabDestinations

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                VelmorthBottomNavBar(
                    currentDestClass = currentDestClass,
                    onNavigate = { dest ->
                        // Remove tab destinations from back stack, then add new tab
                        val tabKeys = mainTabDestinations
                        backStack.removeAll { item -> item::class in tabKeys }
                        backStack.add(dest)
                    },
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {

                entry<Splash> {
                    val splashViewModel: SplashViewModel = hiltViewModel()
                    SplashScreen(
                        onNavigateToOnboarding = {
                            splashViewModel.determineDestination(
                                onNavigateToHome = {
                                    backStack.add(Home)
                                    backStack.remove(Splash)
                                },
                                onNavigateToOnboarding = {
                                    backStack.add(Onboarding)
                                    backStack.remove(Splash)
                                },
                            )
                        }
                    )
                }

                entry<Onboarding> {
                    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        onComplete = { languageId, nativeLanguageId, dailyGoalMinutes ->
                            onboardingViewModel.saveOnboardingData(languageId, nativeLanguageId, dailyGoalMinutes)
                            backStack.removeAll { true }
                            backStack.add(Home)
                        }
                    )
                }


                entry<Home> {
                    HomeScreen(
                        onStartLesson = { backStack.add(Lessons) },
                        onNavigateToProfile = { backStack.add(Profile) },
                    )
                }

                entry<Lessons> {
                    LessonPathScreen(
                        onStartLesson = { lessonId ->
                            backStack.add(LessonPlayer(lessonId))
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<LessonPlayer> { key ->
                    LessonPlayerScreen(
                        lessonId = key.lessonId,
                        onComplete = { score ->
                            backStack.removeLastOrNull()
                            backStack.add(Quiz(key.lessonId))
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<ReviewGarden> {
                    ReviewGardenScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Quiz> { key ->
                    QuizScreen(
                        lessonId = key.lessonId,
                        onComplete = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<AISpeaker> {
                    val aiNavViewModel: AISpeakerNavViewModel = hiltViewModel()
                    val isPremium by aiNavViewModel.isPremium.collectAsStateWithLifecycle()
                    AISpeakerScreen(
                        isPremium = isPremium,
                        onUpgradeToPremium = { backStack.add(Premium) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<LeafShop> {
                    LeafShopScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Premium> {
                    PremiumPaywallScreen(
                        onSubscribe = { _ -> backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Profile> {
                    ProfileScreen(
                        onNavigateToSettings = { backStack.add(Settings) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<Settings> {
                    SettingsScreen(
                        onNavigateToPremium = { backStack.add(Premium) },
                        onBack = { backStack.removeLastOrNull() },
                        onResetProgress = {
                            backStack.removeAll { true }
                            backStack.add(Splash)
                        }
                    )
                }

                // Legacy
                entry<Main> {
                    HomeScreen(
                        onStartLesson = { backStack.add(Lessons) },
                        onNavigateToProfile = { backStack.add(Profile) },
                    )
                }
            }
        )
    }
}

// =============================================
// Forest-styled Bottom Navigation Bar
// =============================================
@Composable
fun VelmorthBottomNavBar(
    currentDestClass: kotlin.reflect.KClass<*>?,
    onNavigate: (NavKey) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
    ) {
        bottomNavItems.forEach { dest ->
            val selected = when (dest) {
                BottomNavDest.HomeTab -> currentDestClass == Home::class
                BottomNavDest.LessonsTab -> currentDestClass == Lessons::class
                BottomNavDest.ReviewTab -> currentDestClass == ReviewGarden::class
                BottomNavDest.ShopTab -> currentDestClass == LeafShop::class
                BottomNavDest.ProfileTab -> currentDestClass == Profile::class
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.navKey) },
                icon = {
                    Text(
                        text = dest.icon,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
