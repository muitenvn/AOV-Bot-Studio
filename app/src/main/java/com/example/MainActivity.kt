package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ExportAndGithubScreen
import com.example.ui.screens.MicroAiTrainerScreen
import com.example.ui.screens.RealGameControllerScreen
import com.example.ui.screens.RuleBuilderScreen
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MutedSteelGray
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianNavy
import com.example.viewmodel.BotStudioViewModel

private data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AovBotStudioApp()
            }
        }
    }
}

@Composable
fun AovBotStudioApp(
    viewModel: BotStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val realServiceState by viewModel.realServiceState.collectAsStateWithLifecycle()
    val shizukuState by viewModel.shizukuState.collectAsStateWithLifecycle()
    val testInput by viewModel.testInputState.collectAsStateWithLifecycle()
    val lastTrainingReport by viewModel.lastTrainingReport.collectAsStateWithLifecycle()
    val touchCoords by viewModel.touchCoords.collectAsStateWithLifecycle()

    val activeProfile = profiles.firstOrNull { it.id == selectedId }
        ?: profiles.firstOrNull()
        ?: viewModel.getActiveProfile()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    val tabs = listOf(
        NavTabItem("Chạy Game Thật", Icons.Filled.Radar, Icons.Outlined.Radar, "nav_tab_real_game"),
        NavTabItem("Múa & Đi Đường", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_tab_rules"),
        NavTabItem("Meta & Dạy AI", Icons.Filled.Memory, Icons.Outlined.Memory, "nav_tab_ai"),
        NavTabItem("Tọa Độ & GitHub", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload, "nav_tab_export")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianNavy,
        bottomBar = {
            NavigationBar(
                containerColor = DeepSlateBlue
            ) {
                tabs.forEachIndexed { index, item ->
                    val selected = currentTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(text = item.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianNavy,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan,
                            unselectedIconColor = MutedSteelGray,
                            unselectedTextColor = MutedSteelGray
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            0 -> RealGameControllerScreen(
                profiles = profiles,
                activeProfile = activeProfile,
                serviceState = realServiceState,
                shizukuState = shizukuState,
                onSelectProfile = viewModel::selectProfile,
                onChangeBrainMode = viewModel::updateActiveBrainMode,
                onToggleAutoPlay = viewModel::toggleRealGameAutoPlay,
                onUpdateScanInterval = viewModel::updateRealScanInterval,
                onToggleOverlay = viewModel::toggleRealFloatingOverlay,
                onConnectShizuku = { viewModel.connectShizuku(context) },
                onUnlockAccessibilityViaShizuku = { viewModel.unlockAccessibilityViaShizuku(context) },
                onToggleShizukuDirectLoop = { enabled -> viewModel.toggleShizukuDirectLoop(context, enabled) },
                modifier = Modifier.padding(innerPadding)
            )

            1 -> RuleBuilderScreen(
                profiles = profiles,
                activeProfile = activeProfile,
                onSelectProfile = viewModel::selectProfile,
                onSaveProfile = viewModel::saveProfile,
                onCreateNewProfile = viewModel::createNewPresetProfile,
                onDeleteProfile = viewModel::deleteProfile,
                modifier = Modifier.padding(innerPadding)
            )

            2 -> MicroAiTrainerScreen(
                activeProfile = activeProfile,
                testInput = testInput,
                lastTrainingReport = lastTrainingReport,
                onUpdateTestInput = viewModel::updateTestInput,
                onUpdateEnemyCompTags = viewModel::updateEnemyCompTags,
                onTrainEpochs = viewModel::trainMicroAiOnDevice,
                modifier = Modifier.padding(innerPadding)
            )

            3 -> ExportAndGithubScreen(
                activeProfile = activeProfile,
                touchCoords = touchCoords,
                onUpdateCoord = viewModel::updateTouchCoord,
                exportConfigJson = viewModel.buildExportConfigScript(activeProfile),
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
