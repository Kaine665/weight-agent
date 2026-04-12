package com.weightagent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.weightagent.app.ui.WeightAgentAppRoot
import com.weightagent.app.ui.theme.WeightAgentTheme
import com.weightagent.app.work.RefreshAndEnqueueWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightAgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    WeightAgentAppRoot(navController = navController)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            RefreshAndEnqueueWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            RefreshAndEnqueueWorker.buildRequest(),
        )
    }
}
