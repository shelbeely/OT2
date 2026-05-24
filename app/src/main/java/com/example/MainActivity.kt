package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TransitionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TransitionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isLocked by viewModel.isLocked.collectAsState()

                if (isLocked) {
                    AppLockScreen(viewModel = viewModel)
                } else {
                    MainNavigationContainer(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.isLaunchingIntent = false
    }

    override fun onStop() {
        super.onStop()
        // Lock the application when putting it into background to ensure strict local privacy!
        if (!viewModel.isLaunchingIntent) {
            viewModel.lockApp()
        }
    }
}

@Composable
fun MainNavigationContainer(viewModel: TransitionViewModel) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    val navigationItems = listOf(
        NavigationTabItem(
            label = "Home",
            selectedIcon = Icons.Default.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard
        ),
        NavigationTabItem(
            label = "Gallery",
            selectedIcon = Icons.Default.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary
        ),
        NavigationTabItem(
            label = "Voice",
            selectedIcon = Icons.Default.Mic,
            unselectedIcon = Icons.Outlined.Mic
        ),
        NavigationTabItem(
            label = "Timeline",
            selectedIcon = Icons.Default.Flag,
            unselectedIcon = Icons.Outlined.Flag
        ),
        NavigationTabItem(
            label = "Settings",
            selectedIcon = Icons.Default.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                navigationItems.forEachIndexed { index, item ->
                    val isSelected = selectedScreenIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { 
                            currentSubScreen = null
                            selectedScreenIndex = index 
                        },
                        label = { Text(item.label) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSubScreen == "health_connect") {
                HealthConnectScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentSubScreen = null }
                )
            } else {
                when (selectedScreenIndex) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCamera = { selectedScreenIndex = 1 },
                        onNavigateToVoice = { selectedScreenIndex = 2 },
                        onNavigateToMilestones = { selectedScreenIndex = 3 },
                        onNavigateToHealthConnect = { currentSubScreen = "health_connect" }
                    )
                    1 -> GalleryScreen(viewModel = viewModel)
                    2 -> VoiceRecorderScreen(viewModel = viewModel)
                    3 -> MilestonesScreen(viewModel = viewModel)
                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToHealthConnect = { currentSubScreen = "health_connect" }
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
