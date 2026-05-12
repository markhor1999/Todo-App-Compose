package com.codingwithsalman.apps.todo.app.compose.jotjives.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation.NavigationRoot
import com.codingwithsalman.jotjive.core.presentation.designsystem.theme.JotJiveTheme

class MainActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JotJiveTheme {
                val navController = rememberNavController()
                NavigationRoot(navController)
            }
        }
    }
}