package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BotProfileEntity
import com.example.data.BotRepository
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.data.TargetPriority
import com.example.engine.ArenaSimulationEngine
import com.example.engine.ArenaSnapshot
import com.example.engine.BrainDecisionResult
import com.example.engine.MicroBotBrain
import com.example.engine.TacticalInputState
import com.example.engine.TrainingReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TouchButtonCoord(
    val id: String,
    val labelVi: String,
    val xPercent: Int,
    val yPercent: Int
)

class BotStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BotRepository
    private val arenaEngine = ArenaSimulationEngine()

    val profiles: StateFlow<List<BotProfileEntity>>

    private val _selectedProfileId = MutableStateFlow<Int?>(null)
    val selectedProfileId: StateFlow<Int?> = _selectedProfileId.asStateFlow()

    private val _arenaState = MutableStateFlow(ArenaSnapshot())
    val arenaState: StateFlow<ArenaSnapshot> = _arenaState.asStateFlow()

    private val _simulationSpeedMultiplier = MutableStateFlow(1)
    val simulationSpeedMultiplier: StateFlow<Int> = _simulationSpeedMultiplier.asStateFlow()

    // State cho màn hình Huấn luyện Micro-AI trên thiết bị
    private val _testInputState = MutableStateFlow(
        TacticalInputState(
            botHpRatio = 0.72f,
            botManaRatio = 0.65f,
            enemyHpRatio = 0.28f,
            distToEnemyNorm = 0.32f,
            distToAllyTowerNorm = 0.45f,
            enemyUnderTower = 0.0f,
            skillsReadyRatio = 1.0f,
            incomingProjectileThreat = 0.0f
        )
    )
    val testInputState: StateFlow<TacticalInputState> = _testInputState.asStateFlow()

    private val _lastTrainingReport = MutableStateFlow<TrainingReport?>(null)
    val lastTrainingReport: StateFlow<TrainingReport?> = _lastTrainingReport.asStateFlow()

    private val _touchCoords = MutableStateFlow(
        listOf(
            TouchButtonCoord("JOYSTICK", "Tâm Joystick Di chuyển", 18, 76),
            TouchButtonCoord("AA_HERO", "Nút Đánh thường (Chính)", 88, 82),
            TouchButtonCoord("AA_MINION", "Nút Đánh Lính (Bổ trợ)", 79, 89),
            TouchButtonCoord("AA_TOWER", "Nút Đẩy Trụ (Bổ trợ)", 93, 66),
            TouchButtonCoord("SKILL_1", "Chiêu 1 (Định hướng)", 73, 78),
            TouchButtonCoord("SKILL_2", "Chiêu 2 (Khống chế/Lướt)", 78, 61),
            TouchButtonCoord("SKILL_3", "Chiêu 3 (Ultimate)", 87, 50),
            TouchButtonCoord("SPELL_HEAL", "Phép Bổ Trợ (Tốc biến/Cấp cứu)", 64, 86),
            TouchButtonCoord("QUICK_SHOP", "Ô Mua Đồ Nhanh #1", 11, 42)
        )
    )
    val touchCoords: StateFlow<List<TouchButtonCoord>> = _touchCoords.asStateFlow()

    private var cachedWeightsProfileId: Int = -1
    private var cachedWeightsArray: FloatArray = MicroBotBrain.createPresetWeights(HeroRole.MARKSMAN)

    init {
        val dao = AppDatabase.getInstance(application).botDao()
        repository = BotRepository(dao)
        profiles = repository.allProfiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultProfilesIfNeeded()
        }

        // Vòng lặp mô phỏng Sân Đấu thời gian thực (20 nhịp/giây, có thể tăng tốc x2)
        viewModelScope.launch {
            while (isActive) {
                val activeProfile = getActiveProfile()
                if (activeProfile.id != cachedWeightsProfileId) {
                    cachedWeightsProfileId = activeProfile.id
                    cachedWeightsArray = MicroBotBrain.parseWeightsOrDefault(activeProfile)
                }
                val mult = _simulationSpeedMultiplier.value
                repeat(mult) {
                    _arenaState.value = arenaEngine.stepSimulation(
                        current = _arenaState.value,
                        profile = activeProfile,
                        cachedWeights = cachedWeightsArray,
                        dtSec = 0.05f
                    )
                }
                delay(50L)
            }
        }
    }

    fun getActiveProfile(): BotProfileEntity {
        val list = profiles.value
        val selId = _selectedProfileId.value
        return list.firstOrNull { it.id == selId }
            ?: list.firstOrNull()
            ?: BotProfileEntity(
                id = 0,
                name = "Valhein / Hayate - Xạ Thủ Thả Diều",
                heroRole = HeroRole.MARKSMAN.name,
                brainMode = BrainMode.HYBRID.name
            )
    }

    fun selectProfile(id: Int) {
        _selectedProfileId.value = id
        cachedWeightsProfileId = -1
    }

    fun updateActiveBrainMode(mode: BrainMode) {
        val current = getActiveProfile()
        val updated = current.copy(brainMode = mode.name, updatedAt = System.currentTimeMillis())
        saveProfile(updated)
    }

    fun saveProfile(profile: BotProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val weights = if (profile.weightsCsv.isBlank()) {
                val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
                MicroBotBrain.weightsToCsv(MicroBotBrain.createPresetWeights(role))
            } else {
                profile.weightsCsv
            }
            val toSave = profile.copy(weightsCsv = weights, updatedAt = System.currentTimeMillis())
            if (toSave.id == 0) {
                val newId = repository.insert(toSave).toInt()
                _selectedProfileId.value = newId
            } else {
                repository.update(toSave)
            }
            cachedWeightsProfileId = -1
        }
    }

    fun createNewPresetProfile(name: String, role: HeroRole, mode: BrainMode) {
        viewModelScope.launch(Dispatchers.IO) {
            val weights = MicroBotBrain.createPresetWeights(role)
            val newProfile = BotProfileEntity(
                name = name.ifBlank { "Bot ${role.displayName}" },
                heroRole = role.name,
                brainMode = mode.name,
                retreatHpPercent = if (role == HeroRole.MARKSMAN || role == HeroRole.MAGE) 28 else 20,
                diveTowerEnemyHpPercent = if (role == HeroRole.ASSASSIN) 20 else 14,
                kitingDistance = role.baseRange.toInt(),
                targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                comboSequence = if (role == HeroRole.ASSASSIN) "S2 -> S3 -> S1 -> AA" else "S1 -> S2 -> AA -> S3",
                weightsCsv = MicroBotBrain.weightsToCsv(weights),
                trainedEpochs = 300,
                lastAccuracyPercent = 93.5f
            )
            val id = repository.insert(newProfile).toInt()
            _selectedProfileId.value = id
            cachedWeightsProfileId = -1
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (profiles.value.size > 1) {
                repository.deleteById(id)
                if (_selectedProfileId.value == id) {
                    _selectedProfileId.value = profiles.value.firstOrNull { it.id != id }?.id
                }
            }
        }
    }

    fun toggleArenaPause() {
        _arenaState.value = _arenaState.value.copy(isPaused = !_arenaState.value.isPaused)
    }

    fun cycleSimulationSpeed() {
        _simulationSpeedMultiplier.value = if (_simulationSpeedMultiplier.value == 1) 2 else 1
    }

    fun resetArenaMatch() {
        _arenaState.value = ArenaSnapshot()
    }

    fun triggerTacticalScenario(scenarioType: Int) {
        _arenaState.value = arenaEngine.triggerScenario(_arenaState.value, scenarioType)
    }

    fun updateTestInput(updater: (TacticalInputState) -> TacticalInputState) {
        _testInputState.value = updater(_testInputState.value)
    }

    fun evaluateTestSandbox(): BrainDecisionResult {
        val active = getActiveProfile()
        val weights = MicroBotBrain.parseWeightsOrDefault(active)
        return MicroBotBrain.decide(_testInputState.value, active, weights)
    }

    fun trainMicroAiOnDevice(epochs: Int) {
        val active = getActiveProfile()
        val role = runCatching { HeroRole.valueOf(active.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
        val currentWeights = MicroBotBrain.parseWeightsOrDefault(active)
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                MicroBotBrain.trainOnDevice(
                    initialWeights = currentWeights,
                    role = role,
                    retreatHpPercent = active.retreatHpPercent,
                    epochs = epochs
                )
            }
            _lastTrainingReport.value = report
            cachedWeightsArray = report.updatedWeights
            val updatedProfile = active.copy(
                weightsCsv = MicroBotBrain.weightsToCsv(report.updatedWeights),
                trainedEpochs = active.trainedEpochs + epochs,
                lastAccuracyPercent = report.accuracyPercent,
                updatedAt = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                if (updatedProfile.id != 0) {
                    repository.update(updatedProfile)
                }
            }
        }
    }

    fun updateTouchCoord(id: String, newX: Int, newY: Int) {
        _touchCoords.value = _touchCoords.value.map {
            if (it.id == id) it.copy(xPercent = newX.coerceIn(5, 95), yPercent = newY.coerceIn(5, 95))
            else it
        }
    }

    fun buildExportConfigScript(profile: BotProfileEntity): String {
        val coordsJson = _touchCoords.value.joinToString(",\n    ") {
            "\"${it.id}\": { \"xPct\": ${it.xPercent}, \"yPct\": ${it.yPercent}, \"label\": \"${it.labelVi}\" }"
        }
        return """
{
  "botProfile": "${profile.name}",
  "heroRole": "${profile.heroRole}",
  "brainEngine": "${profile.brainMode}",
  "rules": {
    "retreatHpPercent": ${profile.retreatHpPercent},
    "diveTowerEnemyHpPercent": ${profile.diveTowerEnemyHpPercent},
    "kitingDistancePx": ${profile.kitingDistance},
    "targetPriority": "${profile.targetPriority}",
    "comboSequence": "${profile.comboSequence}",
    "autoDodgePerpendicularVector": ${profile.autoDodgeSkillshot},
    "autoJungleRotation": ${profile.autoJungleWhenLaneClear}
  },
  "microMlpModel": {
    "architecture": "8-6-5 LeakyReLU + Softmax (89 float weights)",
    "trainedEpochs": ${profile.trainedEpochs},
    "accuracyPercent": ${"%.1f".format(java.util.Locale.US, profile.lastAccuracyPercent)}
  },
  "screenLayoutCoordinates": {
    $coordsJson
  }
}
        """.trimIndent()
    }
}
