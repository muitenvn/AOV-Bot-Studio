package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.engine.AovHeroAndMetaDatabase
import com.example.engine.BotTacticalAction
import com.example.engine.BrainDecisionResult
import com.example.engine.MicroBotBrain
import com.example.engine.RealScreenScanResult
import com.example.engine.ScreenPixelAnalyzer
import com.example.viewmodel.TouchButtonCoord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class RealServiceState(
    val isServiceConnected: Boolean = false,
    val isAutoPlaying: Boolean = false,
    val isOverlayShown: Boolean = true,
    val scanIntervalMs: Long = 350L,
    val framesScanned: Long = 0L,
    val gesturesDispatched: Long = 0L,
    val lastScanTimeMicros: Float = 0f,
    val lastBrainTimeMicros: Float = 0f,
    val detectedBotHpPercent: Int = 82,
    val detectedEnemyHpPercent: Int = 60,
    val enemyLockedOnScreen: Boolean = false,
    val minimapMiaCount: Int = 1,
    val combatWinConfidencePct: Int = 72,
    val recommendedCounterItem: String = "Giày Kiên Cường + Đao Truy Hồn",
    val currentActionTitle: String = "Đang chờ bật Tự Chơi",
    val currentActionReason: String = "Bật Quyền Hỗ Trợ hoặc Shizuku để bắt đầu quét màn hình Liên Quân thật",
    val lastGestureSummary: String = "Chưa gửi thao tác",
    val liveLogs: List<String> = emptyList()
)

class AovBotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: AovBotAccessibilityService? = null

        private val _serviceState = MutableStateFlow(RealServiceState())
        val serviceState: StateFlow<RealServiceState> = _serviceState.asStateFlow()

        @Volatile
        var activeProfileConfig: BotProfileEntity = BotProfileEntity(
            id = 1,
            name = "Florentino - Múa Nhặt Hoa Đường Caesar",
            selectedHeroId = "FLORENTINO",
            assignedLane = "TOP_CAESAR",
            heroRole = HeroRole.WARRIOR.name,
            brainMode = BrainMode.HYBRID.name
        )

        @Volatile
        var activeWeights: FloatArray = MicroBotBrain.createPresetWeights(HeroRole.WARRIOR)

        @Volatile
        var activeTouchCoords: List<TouchButtonCoord> = listOf(
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

        fun setAutoPlaying(enabled: Boolean) {
            _serviceState.value = _serviceState.value.copy(
                isAutoPlaying = enabled,
                currentActionTitle = if (enabled) "Đang Quét Màn Hình & Tự Chơi..." else "Đã Tạm Dừng Bot"
            )
            instance?.updateOverlayUi()
        }

        fun setScanIntervalMs(intervalMs: Long) {
            _serviceState.value = _serviceState.value.copy(
                scanIntervalMs = intervalMs.coerceIn(160L, 1200L)
            )
        }

        fun toggleFloatingOverlay(show: Boolean) {
            _serviceState.value = _serviceState.value.copy(isOverlayShown = show)
            instance?.applyOverlayVisibility(show)
        }

        fun recordExternalShizukuTick(
            scan: RealScreenScanResult,
            decision: BrainDecisionResult,
            gestureSummary: String
        ) {
            val logs = _serviceState.value.liveLogs.toMutableList()
            val entry = String.format(
                Locale.US,
                "[Shizuku %.0fµs | Tỷ lệ thắng %d%%] %s -> %s",
                decision.executionMicros,
                decision.combatWinConfidencePct,
                decision.action.titleVi.substringBefore(" ("),
                gestureSummary
            )
            logs.add(0, entry)
            if (logs.size > 15) logs.removeAt(logs.lastIndex)

            _serviceState.value = _serviceState.value.copy(
                framesScanned = _serviceState.value.framesScanned + 1L,
                gesturesDispatched = _serviceState.value.gesturesDispatched + 1L,
                lastScanTimeMicros = scan.scanTimeMicros,
                lastBrainTimeMicros = decision.executionMicros,
                detectedBotHpPercent = (scan.botHpRatio * 100).toInt(),
                detectedEnemyHpPercent = (scan.enemyHpRatio * 100).toInt(),
                enemyLockedOnScreen = scan.enemyDetected,
                minimapMiaCount = scan.minimapMissingEnemiesMia,
                combatWinConfidencePct = decision.combatWinConfidencePct,
                recommendedCounterItem = decision.recommendedCounterItem,
                currentActionTitle = decision.action.titleVi,
                currentActionReason = decision.reasonVi,
                lastGestureSummary = gestureSummary,
                liveLogs = logs
            )
            instance?.updateOverlayUi()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val screenshotExecutor = Executors.newSingleThreadExecutor()
    private var windowManager: WindowManager? = null
    private var overlayRootView: View? = null
    private var statusTextView: TextView? = null
    private var toggleButtonView: TextView? = null
    private var isOverlayMinimized = false
    private var comboStepCounter = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _serviceState.value = _serviceState.value.copy(
            isServiceConnected = true,
            currentActionTitle = "Quyền Hỗ Trợ Đã Kết Nối"
        )
        setupFloatingTacticalOverlay()
        startContinuousScreenScanAndPlayLoop()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        setAutoPlaying(false)
    }

    override fun onDestroy() {
        removeFloatingOverlay()
        serviceScope.cancel()
        screenshotExecutor.shutdownNow()
        instance = null
        _serviceState.value = _serviceState.value.copy(
            isServiceConnected = false,
            isAutoPlaying = false
        )
        super.onDestroy()
    }

    private fun startContinuousScreenScanAndPlayLoop() {
        serviceScope.launch {
            var tickCounter = 0L
            while (isActive) {
                val state = _serviceState.value
                // Nếu đang chạy chế độ Shizuku Direct độc lập thì để ShizukuManagerEngine xử lý
                if (!state.isAutoPlaying || ShizukuManagerEngine.shizukuState.value.isShizukuLoopRunning) {
                    delay(200L)
                    continue
                }

                tickCounter++
                val profile = activeProfileConfig
                val coords = activeTouchCoords

                val scanResult = captureAndAnalyzeScreen(coords)
                val decision = MicroBotBrain.decide(
                    state = scanResult.tacticalInput,
                    profile = profile,
                    cachedWeights = activeWeights
                )

                val gestureDescText = executeTacticalGestureOnRealScreen(
                    decisionAction = decision.action,
                    scan = scanResult,
                    profile = profile,
                    coords = coords,
                    tick = tickCounter
                )

                val currentLogs = _serviceState.value.liveLogs.toMutableList()
                val logLine = String.format(
                    Locale.US,
                    "[Quét %.0fµs | Thắng %d%% | MIA:%d] %s -> %s",
                    scanResult.scanTimeMicros,
                    decision.combatWinConfidencePct,
                    scanResult.minimapMissingEnemiesMia,
                    decision.action.titleVi.substringBefore(" ("),
                    gestureDescText
                )
                currentLogs.add(0, logLine)
                if (currentLogs.size > 15) currentLogs.removeAt(currentLogs.lastIndex)

                _serviceState.value = _serviceState.value.copy(
                    framesScanned = _serviceState.value.framesScanned + 1L,
                    gesturesDispatched = _serviceState.value.gesturesDispatched + 1L,
                    lastScanTimeMicros = scanResult.scanTimeMicros,
                    lastBrainTimeMicros = decision.executionMicros,
                    detectedBotHpPercent = (scanResult.botHpRatio * 100).toInt(),
                    detectedEnemyHpPercent = (scanResult.enemyHpRatio * 100).toInt(),
                    enemyLockedOnScreen = scanResult.enemyDetected,
                    minimapMiaCount = scanResult.minimapMissingEnemiesMia,
                    combatWinConfidencePct = decision.combatWinConfidencePct,
                    recommendedCounterItem = decision.recommendedCounterItem,
                    currentActionTitle = decision.action.titleVi,
                    currentActionReason = decision.reasonVi,
                    lastGestureSummary = gestureDescText,
                    liveLogs = currentLogs
                )

                updateOverlayUi()
                delay(_serviceState.value.scanIntervalMs)
            }
        }
    }

    private suspend fun captureAndAnalyzeScreen(coords: List<TouchButtonCoord>): RealScreenScanResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ScreenPixelAnalyzer.defaultScanResult(15f)
        }
        return suspendCoroutine { cont ->
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    screenshotExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            try {
                                val hwBuffer = screenshot.hardwareBuffer
                                val colorSpace = screenshot.colorSpace
                                val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                                val softBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                                hwBitmap?.recycle()
                                hwBuffer.close()

                                if (softBitmap != null) {
                                    val result = ScreenPixelAnalyzer.analyzeBitmap(softBitmap, coords)
                                    softBitmap.recycle()
                                    cont.resume(result)
                                } else {
                                    cont.resume(ScreenPixelAnalyzer.defaultScanResult(18f))
                                }
                            } catch (_: Throwable) {
                                cont.resume(ScreenPixelAnalyzer.defaultScanResult(20f))
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            cont.resume(ScreenPixelAnalyzer.defaultScanResult(22f))
                        }
                    }
                )
            } catch (_: Throwable) {
                cont.resume(ScreenPixelAnalyzer.defaultScanResult(25f))
            }
        }
    }

    private fun executeTacticalGestureOnRealScreen(
        decisionAction: BotTacticalAction,
        scan: RealScreenScanResult,
        profile: BotProfileEntity,
        coords: List<TouchButtonCoord>,
        tick: Long
    ): String {
        val (screenW, screenH) = getScreenDimensions()
        fun coordPx(id: String, defaultXPct: Int, defaultYPct: Int): Pair<Float, Float> {
            val found = coords.firstOrNull { it.id == id }
            val xp = (found?.xPercent ?: defaultXPct) / 100f
            val yp = (found?.yPercent ?: defaultYPct) / 100f
            return (xp * screenW) to (yp * screenH)
        }

        val (joyX, joyY) = coordPx("JOYSTICK", 18, 76)
        val joyRadius = max(65f, screenW * 0.065f)

        // Tự động mua đồ khắc chế ở ô góc trái
        if (profile.autoBuyEquipment && scan.quickShopAvailable && tick % 8L == 0L) {
            val (shopX, shopY) = coordPx("QUICK_SHOP", 11, 42)
            dispatchTapGesture(shopX, shopY, 45L)
            return "Mua Nhanh Trang Bị Khắc Chế (${shopX.toInt()}, ${shopY.toInt()})"
        }

        return when (decisionAction) {
            BotTacticalAction.SAFE_RECALL_BASE -> {
                val (recallX, recallY) = coordPx("RECALL_BTN", 54, 86)
                if (tick % 2L == 1L) {
                    dispatchSwipeGesture(joyX, joyY, joyX - joyRadius * 0.8f, joyY + joyRadius * 0.8f, 260L)
                    "Kéo Joystick Vào Bụi/Trụ Chuẩn Bị Biến Về"
                } else {
                    dispatchTapGesture(recallX, recallY, 70L)
                    "Bấm Nút Biến Về (${recallX.toInt()}, ${recallY.toInt()}) Hồi Đầy Máu & Mana"
                }
            }

            BotTacticalAction.RETREAT_HEAL -> {
                if (scan.healSpellReady && tick % 3L == 0L) {
                    val (healX, healY) = coordPx("SPELL_HEAL", 64, 86)
                    dispatchSimultaneousJoystickAndTap(
                        joyX, joyY,
                        joyX - joyRadius * 0.82f, joyY + joyRadius * 0.82f,
                        healX, healY,
                        280L
                    )
                    "Bấm Tốc Biến/Cấp Cứu + Kéo Joystick Lùi Trụ"
                } else {
                    dispatchSwipeGesture(
                        joyX, joyY,
                        joyX - joyRadius * 0.85f,
                        joyY + joyRadius * 0.85f,
                        320L
                    )
                    "Kéo Joystick Rút Lui Về Trụ Đồng Minh (Góc 225°)"
                }
            }

            BotTacticalAction.DODGE_SKILLSHOT -> {
                val perpAngle = scan.enemyAngleRad + (PI.toFloat() / 2f)
                val targetJoyX = joyX + cos(perpAngle) * joyRadius
                val targetJoyY = joyY + sin(perpAngle) * joyRadius
                dispatchSwipeGesture(joyX, joyY, targetJoyX, targetJoyY, 210L)
                "Lách Joystick Vuông Góc 90° Né Chiêu Định Hướng"
            }

            BotTacticalAction.KITE_POKE -> {
                val retreatAngle = scan.enemyAngleRad + PI.toFloat()
                val targetJoyX = joyX + cos(retreatAngle) * (joyRadius * 0.75f)
                val targetJoyY = joyY + sin(retreatAngle) * (joyRadius * 0.75f)
                val (btnX, btnY) = if (scan.skill1Ready && tick % 2L == 0L) {
                    coordPx("SKILL_1", 73, 78)
                } else {
                    coordPx("AA_HERO", 88, 82)
                }
                dispatchSimultaneousJoystickAndTap(joyX, joyY, targetJoyX, targetJoyY, btnX, btnY, 240L)
                "Thả Diều Giữ Cự Ly + Bắn Cấu Rỉa"
            }

            BotTacticalAction.ATTACK_COMBO -> {
                // Múa theo đúng cơ chế riêng của từng vị tướng trong AovHeroAndMetaDatabase
                val heroSpec = AovHeroAndMetaDatabase.getHeroSpecByIdOrRole(profile.selectedHeroId, profile.heroRole)
                val step = heroSpec.signatureComboSteps[comboStepCounter % heroSpec.signatureComboSteps.size]
                comboStepCounter++

                val (btnX, btnY) = coordPx(step.buttonId, 88, 82)
                val aimRadius = max(58f, screenW * 0.055f)

                when (step.gestureType) {
                    "SWIPE_REVERSE_ENEMY" -> {
                        val revAngle = scan.enemyAngleRad + PI.toFloat()
                        val endX = btnX + cos(revAngle) * aimRadius
                        val endY = btnY + sin(revAngle) * aimRadius
                        dispatchSwipeGesture(btnX, btnY, endX, endY, 135L)
                    }
                    "SWIPE_TO_ENEMY" -> {
                        val endX = btnX + cos(scan.enemyAngleRad) * aimRadius
                        val endY = btnY + sin(scan.enemyAngleRad) * aimRadius
                        dispatchSwipeGesture(btnX, btnY, endX, endY, 135L)
                    }
                    "JOYSTICK_FLOWER_120" -> {
                        val flowerAngle = scan.enemyAngleRad + (2f * PI.toFloat() / 3f) * (comboStepCounter % 3)
                        val jx = joyX + cos(flowerAngle) * joyRadius
                        val jy = joyY + sin(flowerAngle) * joyRadius
                        dispatchSimultaneousJoystickAndTap(joyX, joyY, jx, jy, btnX, btnY, 175L)
                    }
                    else -> {
                        val jx = joyX + cos(scan.enemyAngleRad) * (joyRadius * 0.65f)
                        val jy = joyY + sin(scan.enemyAngleRad) * (joyRadius * 0.65f)
                        dispatchSimultaneousJoystickAndTap(joyX, joyY, jx, jy, btnX, btnY, 190L)
                    }
                }
                "Múa ${heroSpec.heroName.substringBefore(" ")}: ${step.stepTitle}"
            }

            BotTacticalAction.PUSH_LANE -> {
                val targetJoyX = joyX + joyRadius * 0.75f
                val targetJoyY = joyY - joyRadius * 0.75f
                val (btnX, btnY) = if (tick % 3L == 0L) {
                    coordPx("AA_TOWER", 93, 66)
                } else {
                    coordPx("AA_MINION", 79, 89)
                }
                dispatchSimultaneousJoystickAndTap(joyX, joyY, targetJoyX, targetJoyY, btnX, btnY, 250L)
                "Đi Đường ${profile.assignedLane} Dọn Lính & Đẩy Trụ"
            }

            BotTacticalAction.FARM_JUNGLE -> {
                val (btnX, btnY) = if (scan.skill1Ready && tick % 2L == 0L) {
                    coordPx("SKILL_1", 73, 78)
                } else {
                    coordPx("AA_MINION", 79, 89)
                }
                dispatchTapGesture(btnX, btnY, 70L)
                "Ăn Quái Rừng / Tranh Chấp Mục Tiêu Lớn"
            }

            BotTacticalAction.ROAM_ROTATE -> {
                val targetJoyX = joyX + joyRadius * 0.45f
                val targetJoyY = joyY - joyRadius * 0.85f
                dispatchSwipeGesture(joyX, joyY, targetJoyX, targetJoyY, 280L)
                "Đọc Minimap Đảo Đường Hội Quân Hỗ Trợ Đồng Đội"
            }
        }
    }

    private fun dispatchTapGesture(x: Float, y: Float, durationMs: Long) {
        val (w, h) = getScreenDimensions()
        val safeX = x.coerceIn(10f, w - 10f)
        val safeY = y.coerceIn(10f, h - 10f)
        val path = Path().apply { moveTo(safeX, safeY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long) {
        val (w, h) = getScreenDimensions()
        val path = Path().apply {
            moveTo(startX.coerceIn(10f, w - 10f), startY.coerceIn(10f, h - 10f))
            lineTo(endX.coerceIn(10f, w - 10f), endY.coerceIn(10f, h - 10f))
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchSimultaneousJoystickAndTap(
        joyStartX: Float,
        joyStartY: Float,
        joyEndX: Float,
        joyEndY: Float,
        tapX: Float,
        tapY: Float,
        durationMs: Long
    ) {
        val (w, h) = getScreenDimensions()
        val joyPath = Path().apply {
            moveTo(joyStartX.coerceIn(10f, w - 10f), joyStartY.coerceIn(10f, h - 10f))
            lineTo(joyEndX.coerceIn(10f, w - 10f), joyEndY.coerceIn(10f, h - 10f))
        }
        val tapPath = Path().apply {
            moveTo(tapX.coerceIn(10f, w - 10f), tapY.coerceIn(10f, h - 10f))
        }
        val joyStroke = GestureDescription.StrokeDescription(joyPath, 0L, durationMs)
        val tapStroke = GestureDescription.StrokeDescription(tapPath, 20L, 65L)
        val gesture = GestureDescription.Builder()
            .addStroke(joyStroke)
            .addStroke(tapStroke)
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun getScreenDimensions(): Pair<Float, Float> {
        val dm: DisplayMetrics = resources.displayMetrics
        return dm.widthPixels.toFloat().coerceAtLeast(360f) to dm.heightPixels.toFloat().coerceAtLeast(360f)
    }

    private fun setupFloatingTacticalOverlay() {
        CoroutineScope(Dispatchers.Main).launch {
            if (overlayRootView != null) return@launch
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager = wm

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 36
                    y = 80
                }

                val container = LinearLayout(this@AovBotAccessibilityService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(24, 14, 24, 14)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#E60D1322"))
                        cornerRadius = 42f
                        setStroke(3, Color.parseColor("#00E5FF"))
                    }
                }

                val btnToggle = TextView(this@AovBotAccessibilityService).apply {
                    text = "▶ CHẠY BOT"
                    setTextColor(Color.parseColor("#0D1322"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(22, 10, 22, 10)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#00E676"))
                        cornerRadius = 30f
                    }
                    setOnClickListener {
                        val nextState = !_serviceState.value.isAutoPlaying
                        setAutoPlaying(nextState)
                    }
                }
                toggleButtonView = btnToggle

                val tvStatus = TextView(this@AovBotAccessibilityService).apply {
                    text = "Sẵn sàng quét màn hình"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(20, 0, 16, 0)
                }
                statusTextView = tvStatus

                val btnMin = TextView(this@AovBotAccessibilityService).apply {
                    text = "[–]"
                    setTextColor(Color.parseColor("#FFD54F"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(10, 4, 6, 4)
                    setOnClickListener {
                        isOverlayMinimized = !isOverlayMinimized
                        tvStatus.visibility = if (isOverlayMinimized) View.GONE else View.VISIBLE
                        text = if (isOverlayMinimized) "[+]" else "[–]"
                    }
                }

                var initialX = 0
                var initialY = 0
                var initialTouchX = 0f
                var initialTouchY = 0f
                container.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            runCatching { wm.updateViewLayout(container, params) }
                            true
                        }
                        else -> false
                    }
                }

                container.addView(btnToggle)
                container.addView(tvStatus)
                container.addView(btnMin)

                wm.addView(container, params)
                overlayRootView = container
            } catch (_: Throwable) {
            }
        }
    }

    private fun updateOverlayUi() {
        CoroutineScope(Dispatchers.Main).launch {
            val st = _serviceState.value
            toggleButtonView?.apply {
                text = if (st.isAutoPlaying) "⏸ DỪNG BOT" else "▶ CHẠY BOT"
                background = GradientDrawable().apply {
                    setColor(
                        if (st.isAutoPlaying) Color.parseColor("#FF5252")
                        else Color.parseColor("#00E676")
                    )
                    cornerRadius = 30f
                }
                setTextColor(if (st.isAutoPlaying) Color.WHITE else Color.parseColor("#0D1322"))
            }
            statusTextView?.text = if (st.isAutoPlaying) {
                "Máu ${st.detectedBotHpPercent}% | MIA:${st.minimapMiaCount} | ${st.currentActionTitle.substringBefore(" (")}"
            } else {
                "Đã tạm dừng (${st.framesScanned} khung)"
            }
        }
    }

    private fun applyOverlayVisibility(show: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            overlayRootView?.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun removeFloatingOverlay() {
        CoroutineScope(Dispatchers.Main).launch {
            overlayRootView?.let { view ->
                runCatching { windowManager?.removeView(view) }
            }
            overlayRootView = null
        }
    }
}
