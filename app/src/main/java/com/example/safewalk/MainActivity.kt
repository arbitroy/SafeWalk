package com.example.safewalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.pairing.PairingManager
import com.example.safewalk.permissions.PermissionsManager
import com.example.safewalk.ui.screens.AlertScreen
import com.example.safewalk.ui.screens.DashboardScreen
import com.example.safewalk.ui.screens.EmergencyContactsScreen
import com.example.safewalk.ui.screens.HistoryScreen
import com.example.safewalk.ui.screens.LoginScreen
import com.example.safewalk.ui.screens.OnboardingScreen
import com.example.safewalk.ui.screens.PairingScreen
import com.example.safewalk.ui.screens.ProfileScreen
import com.example.safewalk.ui.screens.SettingsScreen
import com.example.safewalk.ui.screens.SignUpScreen
import com.example.safewalk.ui.theme.SafeWalkTheme
import com.example.safewalk.ui.viewmodel.AuthState
import com.example.safewalk.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: SafeWalkRepository

    @Inject
    lateinit var permissionsManager: PermissionsManager

    @Inject
    lateinit var pairingManager: PairingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request all pairing permissions on app startup
        permissionsManager.registerPermissionLauncher(this)
        permissionsManager.requestAllPairingPermissions(this)

        // Load previously paired device
        pairingManager.loadPairedDevice()

        setContent {
            SafeWalkTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SafeWalkNavigation()
                }
            }
        }
    }
}

// ============================================================================
// Navigation Setup
// ============================================================================

@Composable
fun SafeWalkNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // React to auth state changes and navigate accordingly
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("dashboard") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Idle -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> Unit // CheckingSession or Loading — stay on loading screen
        }
    }

    NavHost(navController = navController, startDestination = "loading") {

        // Splash / session-check screen
        composable("loading") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Authentication Routes
        composable("login") {
            LoginScreen(navController, authViewModel)
        }
        composable("signup") {
            SignUpScreen(navController, authViewModel)
        }

        // App Routes
        composable("dashboard") {
            DashboardScreen(navController)
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable("contacts") {
            EmergencyContactsScreen(navController)
        }
        composable("history") {
            HistoryScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        composable("profile") {
            ProfileScreen(navController, authViewModel)
        }
        composable("pairing") {
            PairingScreen(navController)
        }
        composable("alert/{alertType}") {
            AlertScreen(navController)
        }
    }
}
