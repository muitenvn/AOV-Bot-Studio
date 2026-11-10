package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BotProfileEntity
import com.example.data.BotRepository
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.data.TargetPriority
import com.example.engine.MicroBotBrain
import com.example.engine.TacticalInputState
import com.example.engine.TrainingReport
import com.example.service.AovBotAccessibilityService
import com.example.service.RealServiceState
import com.example.service.ShizukuManagerEngine
import com.example.service.ShizukuStatusState
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

    val profiles: StateFlow<List<BotProfileEntity>>
    val realServiceState: StateFlow<RealServiceState> = AovBotAccessibilityService.serviceState
    val shizukuState: StateFlow<ShizukuStatusState> = ShizukuManagerEngine.shizukuState

    private val _selectedProfileId = MutableStateFlow<Int?>(null)
    val selectedProfileId: StateFlow<Int?> = _selectedProfileId.asStateFlow()

    private val _testInputState = MutableStateFlow(
        TacticalInputState(
            botHpRatio = 0.78f,
            botManaRatio = 0.65f,
            enemyHpRatio = 0.32f,
            distToEnemyNorm = 0.30f,
            distToAllyTowerNorm = 0.45f,
            enemyUnderTower = 0.0f,
            skillsReadyRatio = 1.0f,
            incomingProjectileThreat = 0.0f,
            missingEnemiesMia = 1,
            alliesNearbyCount = 2,
            enemiesNearbyCount = 1,
            groundFlowerDetected = true
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
            TouchButtonCoord("RECALL_BTN", "Nút Biến Về Hồi Máu/Mana", 54, 86),
            TouchButtonCoord("QUICK_SHOP", "Ô Mua Đồ Nhanh #1", 11, 42)
        )
    )
    val touchCoords: StateFlow<List<TouchButtonCoord>> = _touchCoords.asStateFlow()

    private var cachedWeightsProfileId: Int = -1
    private var cachedWeightsArray: FloatArray = MicroBotBrain.createPresetWeights(HeroRole.WARRIOR)

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
            ShizukuManagerEngine.checkShizukuEnvironment(application)
        }

        // Đồng bộ liên tục cấu hình đang chọn sang Dịch vụ Quyền Hỗ Trợ & Shizuku
        viewModelScope.launch {
            while (isActive) {
                val activeProfile = getActiveProfile()
                if (activeProfile.id != cachedWeightsProfileId) {
                    cachedWeightsProfileId = activeProfile.id
                    cachedWeightsArray = MicroBotBrain.parseWeightsOrDefault(activeProfile)
                }
                AovBotAccessibilityService.activeProfileConfig = activeProfile
                AovBotAccessibilityService.activeWeights = cachedWeightsArray
                AovBotAccessibilityService.activeTouchCoords = _touchCoords.value
                delay(250L)
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
                name = "Florentino - Múa Nhặt Hoa Đường Caesar",
                selectedHeroId = "FLORENTINO",
                assignedLane = "TOP_CAESAR",
                heroRole = HeroRole.WARRIOR.name,
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

    fun updateEnemyCompTags(tagsCsv: String) {
        val current = getActiveProfile()
        val updated = current.copy(enemyCompTagsCsv = tagsCsv, updatedAt = System.currentTimeMillis())
        saveProfile(updated)
    }

    fun toggleRealGameAutoPlay(enabled: Boolean) {
        AovBotAccessibilityService.activeProfileConfig = getActiveProfile()
        AovBotAccessibilityService.activeWeights = cachedWeightsArray
        AovBotAccessibilityService.activeTouchCoords = _touchCoords.value
        AovBotAccessibilityService.setAutoPlaying(enabled)
    }

    fun updateRealScanInterval(intervalMs: Long) {
        AovBotAccessibilityService.setScanIntervalMs(intervalMs)
    }

    fun toggleRealFloatingOverlay(show: Boolean) {
        AovBotAccessibilityService.toggleFloatingOverlay(show)
    }

    fun connectShizuku(context: Context) {
        ShizukuManagerEngine.requestAndConnectShizuku(context)
    }

    fun unlockAccessibilityViaShizuku(context: Context) {
        ShizukuManagerEngine.autoUnlockAndEnableAccessibilityViaShizuku(context)
    }

    fun toggleShizukuDirectLoop(context: Context, enabled: Boolean) {
        ShizukuManagerEngine.toggleShizukuDirectBotLoop(context, enabled)
    }

    fun saveProfile(profile: BotProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val weights = if (profile.weightsCsv.isBlank()) {
                val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.WARRIOR)
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
            AovBotAccessibilityService.activeProfileConfig = toSave
        }
    }

    fun createNewPresetProfile(name: String, role: HeroRole, mode: BrainMode) {
        viewModelScope.launch(Dispatchers.IO) {
            val weights = MicroBotBrain.createPresetWeights(role)
            val newProfile = BotProfileEntity(
                name = name.ifBlank { "Bot ${role.displayName}" },
                selectedHeroId = "FLORENTINO",
                assignedLane = "TOP_CAESAR",
                heroRole = role.name,
                brainMode = mode.name,
                retreatHpPercent = if (role == HeroRole.MARKSMAN || role == HeroRole.MAGE) 28 else 22,
                recallManaPercent = 15,
                diveTowerEnemyHpPercent = if (role == HeroRole.ASSASSIN) 20 else 14,
                kitingDistance = role.baseRange.toInt(),
                minimapMiaAlertCount = 2,
                targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                comboSequence = "S1 -> AA -> S2 -> Hoa -> S3",
                weightsCsv = MicroBotBrain.weightsToCsv(weights),
                trainedEpochs = 500,
                lastAccuracyPercent = 95.0f
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

    fun updateTestInput(updater: (TacticalInputState) -> TacticalInputState) {
        _testInputState.value = updater(_testInputState.value)
    }

    fun trainMicroAiOnDevice(epochs: Int) {
        val active = getActiveProfile()
        val role = runCatching { HeroRole.valueOf(active.heroRole) }.getOrDefault(HeroRole.WARRIOR)
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
            AovBotAccessibilityService.activeWeights = report.updatedWeights
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
        val updated = _touchCoords.value.map {
            if (it.id == id) it.copy(xPercent = newX.coerceIn(5, 95), yPercent = newY.coerceIn(5, 95))
            else it
        }
        _touchCoords.value = updated
        AovBotAccessibilityService.activeTouchCoords = updated
    }

    fun buildExportConfigScript(profile: BotProfileEntity): String {
        val coordsJson = _touchCoords.value.joinToString(",\n    ") {
            "\"${it.id}\": { \"xPct\": ${it.xPercent}, \"yPct\": ${it.yPercent}, \"label\": \"${it.labelVi}\" }"
        }
        return """
{
  "botProfile": "${profile.name}",
  "hero": "${profile.selectedHeroId}",
  "lane": "${profile.assignedLane}",
  "executionBackends": ["SHIZUKU_SHELL_UID_2000", "ACCESSIBILITY_SERVICE"],
  "brainEngine": "${profile.brainMode}",
  "tacticalRules": {
    "recallHpPercent": ${profile.retreatHpPercent},
    "recallManaPercent": ${profile.recallManaPercent},
    "diveTowerEnemyHpPercent": ${profile.diveTowerEnemyHpPercent},
    "minimapMiaAlertCount": ${profile.minimapMiaAlertCount},
    "kitingDistancePx": ${profile.kitingDistance},
    "targetPriority": "${profile.targetPriority}",
    "signatureCombo": "${profile.comboSequence}",
    "enemyCompCounterTags": "${profile.enemyCompTagsCsv}"
  },
  "screenLayoutCoordinates": {
    $coordsJson
  }
}
        """.trimIndent()
    }
}
