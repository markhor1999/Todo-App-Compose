package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.create_jive.CreateJiveRoot
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.jotjives.JotJiveRoot
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.settings.SettingsRoot
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.util.toCreateJiveRoute


const val ACTION_CREATE_JIVE = "com.codingwithsalman.CREATE_JIVE"

@Composable
fun NavigationRoot(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoute.JotJives(
            startRecording = false
        )
    ) {
        composable<NavigationRoute.JotJives>(
            deepLinks = listOf(
                navDeepLink<NavigationRoute.JotJives>(
                    basePath = "https://jotjive.com/jives"
                ) {
                    action = ACTION_CREATE_JIVE
                }
            )
        ) {
            JotJiveRoot(
                onNavigateToCreateJive = { details ->
                    navController.navigate(details.toCreateJiveRoute())
                },
                onNavigateToSettings = {
                    navController.navigate(NavigationRoute.Settings)
                }
            )
        }
        composable<NavigationRoute.CreateJive> {
            CreateJiveRoot(
                onConfirmLeave = navController::navigateUp
            )
        }
        composable<NavigationRoute.Settings> {
            SettingsRoot(
                onGoBack = navController::navigateUp
            )
        }
    }
}