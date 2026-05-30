package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.FarmViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val viewModel: FarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val isDark by viewModel.darkMode.collectAsState()
            
            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val userSession by viewModel.currentUser.collectAsState()
                    val context = LocalContext.current

                    // Collect and display snackbar / Toast messages for save success & errors
                    LaunchedEffect(Unit) {
                        viewModel.uiMessage.collectLatest { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }

                    // Simple transition between Login and Main screens based on Auth state
                    AnimatedContent(
                        targetState = userSession,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "AuthNavigationTransition"
                    ) { session ->
                        if (session == null) {
                            LoginScreen(viewModel = viewModel)
                        } else {
                            MainAppScreen(
                                viewModel = viewModel,
                                onNavigateToForm = { formScreen ->
                                    // Handle dialog triggers inside modular screens directly
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
