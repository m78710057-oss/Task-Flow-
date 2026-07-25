package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Categories : Screen("categories")
    object Settings : Screen("settings")
    object About : Screen("about")
}
