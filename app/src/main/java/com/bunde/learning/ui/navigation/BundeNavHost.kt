package com.bunde.learning.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bunde.learning.ui.components.BundeBottomNavigation
import com.bunde.learning.ui.components.BundeTopBar
import com.bunde.learning.ui.screens.home.HomeScreen
import com.bunde.learning.ui.screens.home.HomeViewModel
import com.bunde.learning.ui.screens.mylearning.MyLearningScreen
import com.bunde.learning.ui.screens.profile.ProfileScreen
import com.bunde.learning.ui.screens.revision.RevisionScreen
import com.bunde.learning.ui.screens.topic.EnterTopicScreen
import com.bunde.learning.ui.screens.topic.TopicReviewScreen
import com.bunde.learning.ui.screens.tutor.InteractiveTutorScreen
import com.bunde.learning.ui.screens.tutor.InteractiveTutorViewModel
import com.bunde.learning.ui.screens.upload.UploadNotesScreen
import com.bunde.learning.ui.theme.BundeBackground

@Composable
fun BundeNavHost(
    navController: NavHostController = rememberNavController(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.MyLearning.route,
        Screen.Revision.route,
        Screen.Profile.route
    )
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        containerColor = BundeBackground,
        topBar = {
            if (shouldShowBottomBar) {
                BundeTopBar(
                    onSettingsClick = {
                        navController.navigate(Screen.Profile.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar) {
                BundeBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onUploadNotesClick = { navController.navigate(Screen.UploadNotes.route) },
                    onEnterTopicClick = { navController.navigate(Screen.EnterTopic.route) },
                    onContinueLearningClick = { topicId ->
                        navController.navigate(Screen.InteractiveTutor.createRoute(topicId))
                    },
                    onMyLearningClick = { navController.navigate(Screen.MyLearning.route) },
                    onRevisionClick = { navController.navigate(Screen.Revision.route) },
                    onSettingsClick = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.MyLearning.route) {
                MyLearningScreen(
                    viewModel = homeViewModel,
                    onTopicClick = { topicId ->
                        navController.navigate(Screen.InteractiveTutor.createRoute(topicId))
                    }
                )
            }

            composable(Screen.Revision.route) {
                RevisionScreen(
                    viewModel = homeViewModel,
                    onStartRecallSession = {
                        // Spaced retrieval recall session
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(Screen.UploadNotes.route) {
                UploadNotesScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicMapGenerated = { topicId ->
                        homeViewModel.loadData()
                        navController.navigate(Screen.TopicReview.createRoute(topicId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.EnterTopic.route) {
                EnterTopicScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicMapGenerated = { topicId ->
                        homeViewModel.loadData()
                        navController.navigate(Screen.TopicReview.createRoute(topicId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.TopicReview.route,
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                TopicReviewScreen(
                    topicId = topicId,
                    onBackToHome = {
                        navController.popBackStack(Screen.Home.route, false)
                    },
                    onStartLesson = { tid ->
                        navController.navigate(Screen.InteractiveTutor.createRoute(tid)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.InteractiveTutor.route,
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                val tutorViewModel: InteractiveTutorViewModel = viewModel()
                InteractiveTutorScreen(
                    topicId = topicId,
                    viewModel = tutorViewModel,
                    onBackClick = {
                        homeViewModel.loadData()
                        navController.popBackStack(Screen.Home.route, false)
                    }
                )
            }
        }
    }
}
