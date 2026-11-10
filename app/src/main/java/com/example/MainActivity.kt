package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ArenaSandboxScreen
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
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val realServiceState by viewModel.realServiceState.collectAsStateWithLifecycle()
    val arena by viewModel.arenaState.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.simulationSpeedMultiplier.collectAsStateWithLifecycle()
    val testInput by viewModel.testInputState.collectAsStateWithLifecycle()
    val lastTrainingReport by viewModel.lastTrainingReport.collectAsStateWithLifecycle()
    val touchCoords by viewModel.touchCoords.collectAsStateWithLifecycle()

    val activeProfile = profiles.firstOrNull { it.id == selectedId }
        ?: profiles.firstOrNull()
        ?: viewModel.getActiveProfile()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var trainerSubTab by rememberSaveable { mutableIntStateOf(0) }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    val tabs = listOf(
        NavTabItem("Game Thật (Auto)", Icons.Filled.Radar, Icons.Outlined.Radar, "nav_tab_real_game"),
        NavTabItem("Lập Trình Luật", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_tab_rules"),
        NavTabItem("Huấn Luyện AI", Icons.Filled.Memory, Icons.Outlined.Memory, "nav_tab_ai"),
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
                onSelectProfile = viewModel::selectProfile,
                onChangeBrainMode = viewModel::updateActiveBrainMode,
                onToggleAutoPlay = viewModel::toggleRealGameAutoPlay,
                onUpdateScanInterval = viewModel::updateRealScanInterval,
                onToggleOverlay = viewModel::toggleRealFloatingOverlay,
                onOpenTrainerSandbox = { currentTab = 2 },
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

            2 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Thanh chuyển đổi trong Phòng Huấn Luyện AI: Huấn luyện mạng nơ-ron vs Sân tập giả lập để dạy AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subTabs = listOf(
                        "1. Huấn Luyện Micro-AI (89 Tham Số)",
                        "2. Sân Tập Giả Lập (Để Dạy & Test AI)"
                    )
                    subTabs.forEachIndexed { idx, label ->
                        val isSel = trainerSubTab == idx
                        Surface(
                            color = if (isSel) ElectricCyan else DeepSlateBlue,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { trainerSubTab = idx }
                                .border(1.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) ObsidianNavy else Color.White,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }

                if (trainerSubTab == 0) {
                    MicroAiTrainerScreen(
                        activeProfile = activeProfile,
                        testInput = testInput,
                        lastTrainingReport = lastTrainingReport,
                        onUpdateTestInput = viewModel::updateTestInput,
                        onTrainEpochs = viewModel::trainMicroAiOnDevice,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ArenaSandboxScreen(
                        profiles = profiles,
                        activeProfile = activeProfile,
                        arena = arena,
                        speedMultiplier = speedMultiplier,
                        onSelectProfile = viewModel::selectProfile,
                        onChangeBrainMode = viewModel::updateActiveBrainMode,
                        onTogglePause = viewModel::toggleArenaPause,
                        onCycleSpeed = viewModel::cycleSimulationSpeed,
                        onResetMatch = viewModel::resetArenaMatch,
                        onTriggerScenario = viewModel::triggerTacticalScenario,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
