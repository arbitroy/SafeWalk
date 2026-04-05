package com.example.safewalk.navigation

sealed class NavigationScreen(val route: String) {
    data object Home : NavigationScreen("home")
    data object Timer : NavigationScreen("timer")
    data object AlertHistory : NavigationScreen("alert_history")
    data object Contacts : NavigationScreen("contacts")
    data object Wearable : NavigationScreen("wearable")
    data object Settings : NavigationScreen("settings")
}