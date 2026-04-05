package com.example.safewalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safewalk.navigation.NavigationScreen
import com.example.safewalk.presentation.ui.screens.*
import com.example.safewalk.presentation.ui.theme.SafeWalkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeWalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFAFAFA)
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavigationScreen.Home.route
                    ) {
                        composable(NavigationScreen.Home.route) {
                            HomeScreen(navController = navController)
                        }

                        composable(NavigationScreen.Timer.route) {
                            TimerScreen()
                        }

                        composable(NavigationScreen.AlertHistory.route) {
                            AlertHistoryScreen(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavigationScreen.Contacts.route) {
                            ContactsScreen()
                        }

                        composable(NavigationScreen.Wearable.route) {
                            WearableScreen()
                        }

                        composable(NavigationScreen.Settings.route) {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}