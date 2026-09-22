package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.SalimNavGraph
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SalimTheme
import com.example.ui.viewmodel.SalimViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalimTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureWhite
                ) {
                    val navController = rememberNavController()
                    val viewModel: SalimViewModel = viewModel()
                    SalimNavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

