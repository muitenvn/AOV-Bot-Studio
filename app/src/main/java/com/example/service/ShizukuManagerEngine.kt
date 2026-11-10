package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import com.example.engine.AovHeroAndMetaDatabase
import com.example.engine.BotTacticalAction
import com.example.engine.MicroBotBrain
import com.example.engine.RealScreenScanResult
import com.example.engine.ScreenPixelAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class ExecutionBackendMode(val titleVi: String, val descVi: String) {
    SHIZUKU_DIRECT(
        "Chạy Trực Tiếp Qua Shizuku / Shell (Không Cần Trợ Năng)",
        "Dùng quyền ADB Shell (UID 2000) của Shizuku để quét screencap & gửi input mượt nhất"
    ),
    ACCESSIBILITY_SERVICE(
        "Chạy Qua Quyền Hỗ Trợ (Accessibility Service)",
        "Dùng takeScreenshot + dispatchGesture đa điểm nguyên bản của Android"
    ),
    HYBRID_AUTO_UNLOCK(
        "Kết Hợp: Dùng Shizuku Mở Khóa Trợ Năng + Đa Điểm",
        "Dùng Shizuku vượt chặn 'Cài đặt hạn chế' Android 13-16 và chạy song song"
    )
}

data class ShizukuStatusState(
    val isShizukuAppInstalled: Boolean = false,
    val isShellPrivilegeGranted: Boolean = false,
    val shellUidInfo: String = "Chưa kết nối Shizuku / Shell",
    val selectedBackend: ExecutionBackendMode = ExecutionBackendMode.HYBRID_AUTO_UNLOCK,
    val isShizukuLoopRunning: Boolean = false,
    val lastStatusMessage: String = "Sẵn sàng kết nối Shizuku (moe.shizuku.privileged.api)"
)

object ShizukuManagerEngine {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var persistentShellProcess: Process? = null
    private var persistentShellStdin: DataOutputStream? = null
    private var comboStepIdx = 0

    private val _shizukuState = MutableStateFlow(ShizukuStatusState())
    val shizukuState: StateFlow<ShizukuStatusState> = _shizukuState.asStateFlow()

    fun checkShizukuEnvironment(context: Context) {
        val pm = context.packageManager
        val installed = runCatching {
            pm.getPackageInfo(SHIZUKU_PACKAGE, 0) != null
        }.getOrDefault(false)

        _shizukuState.value = _shizukuState.value.copy(
            isShizukuAppInstalled = installed
        )
    }

    fun selectBackendMode(mode: ExecutionBackendMode) {
        _shizukuState.value = _shizukuState.value.copy(selectedBackend = mode)
    }

    /**
     * Kết nối & kiểm tra quyền Shizuku / ADB Shell / Root và mở luồng Shell tốc độ cao
     */
    fun requestAndConnectShizuku(context: Context) {
        checkShizukuEnvironment(context)
        scope.launch {
            // Thử mở phiên làm việc ưu tiên: rish / su / sh
            val candidates = listOf("su", "sh")
            for (cmd in candidates) {
                try {
                    val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                    val os = DataOutputStream(proc.outputStream)
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    os.writeBytes("id\n")
                    os.flush()
                    val line = reader.readLine() ?: ""
                    if (line.contains("uid=")) {
                        persistentShellProcess?.destroy()
                        persistentShellProcess = proc
                        persistentShellStdin = os
                        val hasPrivilege = line.contains("uid=0") || line.contains("uid=2000")
                        _shizukuState.value = _shizukuState.value.copy(
                            isShellPrivilegeGranted = true,
                            shellUidInfo = line.trim(),
                            lastStatusMessage = if (hasPrivilege) {
                                "Đã nhận quyền Shell cao cấp ($line) - Chạy trực tiếp siêu mượt!"
                            } else {
                                "Đã kết nối phiên Shell nội bộ ($line) - Hãy cấp quyền trong app Shizuku để đạt UID 2000"
                            }
                        )
                        return@launch
                    }
                } catch (_: Throwable) {
                }
            }
            _shizukuState.value = _shizukuState.value.copy(
                lastStatusMessage = "Vui lòng mở ứng dụng Shizuku và bật dịch vụ (Start via Wireless Debugging)."
            )
        }
    }

    /**
     * Mở trực tiếp ứng dụng Shizuku để người dùng cấp quyền 1 chạm
     */
    fun openShizukuApp(context: Context): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    /**
     * Tuyệt chiêu của Shizuku: Tự động mở khóa "Cài đặt bị hạn chế" trên Android 13-16
     * và tự động bật luôn Quyền Hỗ Trợ (AovBotAccessibilityService) mà không cần vào Cài đặt!
     */
    fun autoUnlockAndEnableAccessibilityViaShizuku(context: Context) {
        scope.launch {
            val pkg = context.packageName
            val serviceComponent = "$pkg/com.example.service.AovBotAccessibilityService"
            val commands = listOf(
                "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
                "settings put secure enabled_accessibility_services $serviceComponent",
                "settings put secure accessibility_enabled 1"
            )
            var ok = false
            for (c in commands) {
                if (execFastShellCommand(c)) ok = true
            }
            _shizukuState.value = _shizukuState.value.copy(
                lastStatusMessage = if (ok) {
                    "Đã gửi lệnh Shizuku mở khóa Cài đặt hạn chế & kích hoạt Dịch vụ Bot!"
                } else {
                    "Không thể ghi Secure Settings - Hãy đảm bảo Shizuku đang chạy ở UID 2000 (ADB)."
                }
            )
        }
    }

    /**
     * Bật / Dừng vòng lặp Bot chạy trực tiếp qua Shizuku Shell (Không cần AccessibilityService)
     */
    fun toggleShizukuDirectBotLoop(context: Context, enabled: Boolean) {
        _shizukuState.value = _shizukuState.value.copy(isShizukuLoopRunning = enabled)
        AovBotAccessibilityService.setAutoPlaying(enabled)
        if (!enabled) return

        val dm = context.resources.displayMetrics
        val screenW = dm.widthPixels.toFloat().coerceAtLeast(720f)
        val screenH = dm.heightPixels.toFloat().coerceAtLeast(360f)

        scope.launch {
            var tick = 0L
            while (isActive && _shizukuState.value.isShizukuLoopRunning) {
                tick++
                val profile = AovBotAccessibilityService.activeProfileConfig
                val coords = AovBotAccessibilityService.activeTouchCoords
                val weights = AovBotAccessibilityService.activeWeights

                // 1. Quét màn hình qua screencap stream hoặc bộ quét nhanh
                val scan = captureScreenViaShizukuOrFallback(coords)

                // 2. Bộ não Bot ra quyết định chiến thuật (Múa tướng, Đi đường, Biến về, Lên đồ)
                val decision = MicroBotBrain.decide(scan.tacticalInput, profile, weights)

                // 3. Gửi thao tác cảm ứng trực tiếp qua luồng Shell của Shizuku
                val cmdSummary = dispatchShizukuShellAction(
                    action = decision.action,
                    scan = scan,
                    heroId = profile.selectedHeroId,
                    roleName = profile.heroRole,
                    autoBuy = profile.autoBuyEquipment,
                    coords = coords,
                    screenW = screenW,
                    screenH = screenH,
                    tick = tick
                )

                AovBotAccessibilityService.recordExternalShizukuTick(
                    scan = scan,
                    decision = decision,
                    gestureSummary = "[Shizuku Shell] $cmdSummary"
                )

                delay(AovBotAccessibilityService.serviceState.value.scanIntervalMs)
            }
        }
    }

    private suspend fun captureScreenViaShizukuOrFallback(
        coords: List<com.example.viewmodel.TouchButtonCoord>
    ): RealScreenScanResult = withContext(Dispatchers.IO) {
        try {
            val proc = ProcessBuilder("sh", "-c", "screencap -p").start()
            val bmp = BitmapFactory.decodeStream(proc.inputStream)
            proc.destroy()
            if (bmp != null) {
                val res = ScreenPixelAnalyzer.analyzeBitmap(bmp, coords)
                bmp.recycle()
                return@withContext res
            }
        } catch (_: Throwable) {
        }
        ScreenPixelAnalyzer.defaultScanResult(14f)
    }

    private fun dispatchShizukuShellAction(
        action: BotTacticalAction,
        scan: RealScreenScanResult,
        heroId: String,
        roleName: String,
        autoBuy: Boolean,
        coords: List<com.example.viewmodel.TouchButtonCoord>,
        screenW: Float,
        screenH: Float,
        tick: Long
    ): String {
        fun px(id: String, defX: Int, defY: Int): Pair<Int, Int> {
            val c = coords.firstOrNull { it.id == id }
            val x = (((c?.xPercent ?: defX) / 100f) * screenW).toInt()
            val y = (((c?.yPercent ?: defY) / 100f) * screenH).toInt()
            return x to y
        }

        val (joyX, joyY) = px("JOYSTICK", 18, 76)
        val joyRad = max(70, (screenW * 0.065f).toInt())

        // Tự động mua đồ nhanh ở góc trái (Bao gồm đồ khắc chế đội hình địch)
        if (autoBuy && (scan.quickShopAvailable || tick % 10L == 0L)) {
            val (shopX, shopY) = px("QUICK_SHOP", 11, 42)
            execFastShellCommand("input tap $shopX $shopY")
            return "Mua Trang Bị Khắc Chế Nhanh ($shopX, $shopY)"
        }

        return when (action) {
            BotTacticalAction.SAFE_RECALL_BASE -> {
                val (recallX, recallY) = px("RECALL_BTN", 54, 86)
                // Lùi vào trụ/bụi 1 nhịp rồi bấm nút Biến Về
                if (tick % 2L == 1L) {
                    execFastShellCommand("input swipe $joyX $joyY ${joyX - joyRad} ${joyY + joyRad} 280")
                    "Lùi Vào Bụi/Trụ Chuẩn Bị Biến Về"
                } else {
                    execFastShellCommand("input tap $recallX $recallY")
                    "Bấm Nút Biến Về ($recallX, $recallY) Hồi Đầy Máu & Mana"
                }
            }

            BotTacticalAction.RETREAT_HEAL -> {
                val (healX, healY) = px("SPELL_HEAL", 64, 86)
                execFastShellCommand("input tap $healX $healY & input swipe $joyX $joyY ${joyX - joyRad} ${joyY + joyRad} 300")
                "Bấm Cấp Cứu/Tốc Biến + Kéo Joystick Rút Về Trụ"
            }

            BotTacticalAction.DODGE_SKILLSHOT -> {
                val perpAngle = scan.enemyAngleRad + (PI.toFloat() / 2f)
                val endX = joyX + (cos(perpAngle) * joyRad).toInt()
                val endY = joyY + (sin(perpAngle) * joyRad).toInt()
                execFastShellCommand("input swipe $joyX $joyY $endX $endY 190")
                "Lách Joystick Vuông Góc 90° Né Chiêu Định Hướng"
            }

            BotTacticalAction.ATTACK_COMBO -> {
                // Múa theo đúng bộ cơ chế riêng của từng vị tướng (Florentino nhặt hoa, Nakroth lướt ngược, Tulen tích ấn...)
                val heroSpec = AovHeroAndMetaDatabase.getHeroSpecByIdOrRole(heroId, roleName)
                val step = heroSpec.signatureComboSteps[comboStepIdx % heroSpec.signatureComboSteps.size]
                comboStepIdx++

                val (bx, by) = px(step.buttonId, 88, 82)
                val aimR = max(65, (screenW * 0.055f).toInt())

                when (step.gestureType) {
                    "SWIPE_REVERSE_ENEMY" -> {
                        // Lướt ngược như Chiêu 2 Nakroth hoặc đẩy ngược Thane
                        val revAngle = scan.enemyAngleRad + PI.toFloat()
                        val ex = bx + (cos(revAngle) * aimR).toInt()
                        val ey = by + (sin(revAngle) * aimR).toInt()
                        execFastShellCommand("input swipe $bx $by $ex $ey 130")
                    }
                    "SWIPE_TO_ENEMY" -> {
                        val ex = bx + (cos(scan.enemyAngleRad) * aimR).toInt()
                        val ey = by + (sin(scan.enemyAngleRad) * aimR).toInt()
                        execFastShellCommand("input swipe $bx $by $ex $ey 130")
                    }
                    "JOYSTICK_FLOWER_120" -> {
                        // Bước chéo 120 độ nhặt hoa Florentino
                        val flowerAngle = scan.enemyAngleRad + (2f * PI.toFloat() / 3f) * (comboStepIdx % 3)
                        val ex = joyX + (cos(flowerAngle) * joyRad).toInt()
                        val ey = joyY + (sin(flowerAngle) * joyRad).toInt()
                        execFastShellCommand("input swipe $joyX $joyY $ex $ey 155")
                    }
                    else -> {
                        execFastShellCommand("input tap $bx $by")
                    }
                }
                "Múa ${heroSpec.heroName.substringBefore(" ")}: ${step.stepTitle}"
            }

            BotTacticalAction.KITE_POKE -> {
                val backAngle = scan.enemyAngleRad + PI.toFloat()
                val jx2 = joyX + (cos(backAngle) * joyRad * 0.8f).toInt()
                val jy2 = joyY + (sin(backAngle) * joyRad * 0.8f).toInt()
                val (aaX, aaY) = px("AA_HERO", 88, 82)
                execFastShellCommand("input swipe $joyX $joyY $jx2 $jy2 210 & input tap $aaX $aaY")
                "Thả Diều Giữ Khoảng Cách + Cấu Rỉa"
            }

            BotTacticalAction.PUSH_LANE -> {
                val (btnX, btnY) = if (tick % 3L == 0L) px("AA_TOWER", 93, 66) else px("AA_MINION", 79, 89)
                execFastShellCommand("input swipe $joyX $joyY ${joyX + joyRad} ${joyY - joyRad} 240 & input tap $btnX $btnY")
                "Đi Đường Dọn Lính & Bấm Nút Đẩy Trụ"
            }

            BotTacticalAction.FARM_JUNGLE -> {
                val (mX, mY) = px("AA_MINION", 79, 89)
                execFastShellCommand("input tap $mX $mY")
                "Ăn Bùa Rừng / Tranh Chấp Rồng & Caesar"
            }

            BotTacticalAction.ROAM_ROTATE -> {
                execFastShellCommand("input swipe $joyX $joyY ${joyX + joyRad / 2} ${joyY - joyRad} 280")
                "Nhìn Minimap Đảo Đường Hội Quân Hỗ Trợ Đồng Đội"
            }
        }
    }

    private fun execFastShellCommand(cmd: String): Boolean {
        return try {
            val stdin = persistentShellStdin
            if (stdin != null) {
                stdin.writeBytes("$cmd\n")
                stdin.flush()
                true
            } else {
                val p = ProcessBuilder("sh", "-c", cmd).start()
                p.waitFor()
                true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
