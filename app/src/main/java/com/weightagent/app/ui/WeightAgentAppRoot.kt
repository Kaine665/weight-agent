package com.weightagent.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.weightagent.app.ui.nav.Routes

@Composable
fun WeightAgentAppRoot(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            RecordingListScreen(navController = navController)
        }
        composable(Routes.CONFIG) {
            CosConfigScreen(navController = navController)
        }
    }
}
