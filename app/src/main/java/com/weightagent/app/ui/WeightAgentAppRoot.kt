package com.weightagent.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.weightagent.app.ui.nav.Routes
import com.weightagent.app.ui.nav.resolveCloudStartRoute

@Composable
fun WeightAgentAppRoot(navController: NavHostController) {
    val container = (LocalContext.current.applicationContext as com.weightagent.app.WeightAgentApp).container
    var startRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(container) {
        startRoute = resolveCloudStartRoute(container)
    }
    val start = startRoute
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LIST) {
            RecordingListScreen(navController = navController)
        }
        composable(Routes.CLOUD_SELECT) {
            CloudStorageSelectScreen(navController = navController)
        }
        composable(Routes.CLOUD_HUB) {
            CloudHubScreen(navController = navController)
        }
        composable(Routes.CONFIG_COS) {
            CosConfigScreen(navController = navController)
        }
        composable(Routes.CONFIG_ALIYUN_DRIVE) {
            AliyunDriveConfigScreen(navController = navController)
        }
    }
}
