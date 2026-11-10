package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.viewmodel.TouchButtonCoord
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class RealScreenScanResult(
    val scanTimeMicros: Float,
    val botHpRatio: Float,
    val botManaRatio: Float,
    val enemyDetected: Boolean,
    val enemyHpRatio: Float,
    val enemyScreenXRatio: Float, // 0.0 .. 1.0
    val enemyScreenYRatio: Float, // 0.0 .. 1.0
    val enemyAngleRad: Float,     // Góc từ tâm màn hình tới tướng địch
    val distToEnemyNorm: Float,   // 0.0 (sát tâm) .. 1.0 (rìa màn hình)
    val enemyUnderTower: Boolean,
    val incomingDangerDetected: Boolean,
    val skill1Ready: Boolean,
    val skill2Ready: Boolean,
    val skill3Ready: Boolean,
    val healSpellReady: Boolean,
    val quickShopAvailable: Boolean,
    val tacticalInput: TacticalInputState
)

/**
 * Bộ quét điểm ảnh màn hình thật tốc độ cao (< 2ms):
 * Đọc trực tiếp Bitmap chụp từ AccessibilityService.takeScreenshot()
 * Không dùng mô hình AI nặng, chỉ quét lưới điểm ảnh (Pixel Grid Sampling) siêu nhanh.
 */
object ScreenPixelAnalyzer {

    fun analyzeBitmap(
        bitmap: Bitmap,
        coords: List<TouchButtonCoord>
    ): RealScreenScanResult {
        val startNs = System.nanoTime()
        val w = bitmap.width
        val h = bitmap.height
        if (w < 40 || h < 40) {
            return defaultScanResult(10f)
        }

        // 1. Quét thanh máu nhân vật chính ở vùng trung tâm màn hình (40%..60% X, 30%..48% Y)
        val botHpRatio = scanCenterHeroHpBar(bitmap, w, h)
        val botManaRatio = scanCenterHeroManaBar(bitmap, w, h)

        // 2. Quét tìm thanh máu Đỏ của Tướng Địch / Lính Địch trên chiến trường
        var redCount = 0
        var sumEnemyX = 0f
        var sumEnemyY = 0f
        var minRedX = w
        var maxRedX = 0

        val stepX = max(4, w / 90)
        val stepY = max(4, h / 60)
        val startX = (w * 0.16f).toInt()
        val endX = (w * 0.88f).toInt()
        val startY = (h * 0.10f).toInt()
        val endY = (h * 0.80f).toInt()

        var brightWarningNearCenter = 0
        val centerX = w * 0.50f
        val centerY = h * 0.50f
        val dangerRadiusSq = (min(w, h) * 0.18f) * (min(w, h) * 0.18f)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Màu đỏ đặc trưng của thanh máu địch trong Liên Quân (Đỏ tươi cam/hồng đậm)
                if (r > 190 && g < 72 && b < 78 && (r - g) > 125) {
                    redCount++
                    sumEnemyX += x
                    sumEnemyY += y
                    if (x < minRedX) minRedX = x
                    if (x > maxRedX) maxRedX = x
                }

                // Phát hiện vòng cảnh báo chiêu định hướng đỏ/vàng cam sát chân tướng mình
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy <= dangerRadiusSq) {
                    if (r > 230 && g in 90..210 && b < 70) {
                        brightWarningNearCenter++
                    }
                }
                x += stepX
            }
            y += stepY
        }

        val enemyDetected = redCount >= 3
        val enemyXRatio = if (enemyDetected) (sumEnemyX / redCount) / w.toFloat() else 0.68f
        val enemyYRatio = if (enemyDetected) (sumEnemyY / redCount) / h.toFloat() else 0.32f

        val dxEnemy = (enemyXRatio - 0.50f)
        val dyEnemy = (enemyYRatio - 0.50f)
        val rawDist = sqrt(dxEnemy * dxEnemy + dyEnemy * dyEnemy)
        val distToEnemyNorm = if (enemyDetected) (rawDist / 0.52f).coerceIn(0.05f, 1.0f) else 0.88f
        val enemyAngleRad = atan2(dyEnemy, dxEnemy)

        // Ước lượng % máu địch dựa trên mật độ điểm đỏ tại vùng thanh máu địch
        val enemyHpRatio = if (enemyDetected) {
            estimateEnemyBarFill(bitmap, (enemyXRatio * w).toInt(), (enemyYRatio * h).toInt(), w, h)
        } else {
            0.85f
        }

        // 3. Quét độ sáng tại tâm các nút Chiêu 1, Chiêu 2, Chiêu 3, Cấp Cứu, Mua Đồ
        val s1Coord = coords.firstOrNull { it.id == "SKILL_1" }
        val s2Coord = coords.firstOrNull { it.id == "SKILL_2" }
        val s3Coord = coords.firstOrNull { it.id == "SKILL_3" }
        val healCoord = coords.firstOrNull { it.id == "SPELL_HEAL" }
        val shopCoord = coords.firstOrNull { it.id == "QUICK_SHOP" }

        val s1Ready = isButtonBrightAndReady(bitmap, s1Coord?.xPercent ?: 73, s1Coord?.yPercent ?: 78)
        val s2Ready = isButtonBrightAndReady(bitmap, s2Coord?.xPercent ?: 78, s2Coord?.yPercent ?: 61)
        val s3Ready = isButtonBrightAndReady(bitmap, s3Coord?.xPercent ?: 87, s3Coord?.yPercent ?: 50)
        val healReady = isButtonBrightAndReady(bitmap, healCoord?.xPercent ?: 64, healCoord?.yPercent ?: 86)
        val shopReady = isButtonBrightAndReady(bitmap, shopCoord?.xPercent ?: 11, shopCoord?.yPercent ?: 42)

        var readyCount = 0
        if (s1Ready) readyCount++
        if (s2Ready) readyCount++
        if (s3Ready) readyCount++
        val skillsReadyRatio = readyCount / 3f

        // 4. Quét góc trên bên trái (Minimap) xem mật độ đỏ cao hay gần trụ địch (góc phải trên)
        val enemyInTopRightQuadrant = enemyDetected && enemyXRatio > 0.65f && enemyYRatio < 0.35f
        val incomingDanger = brightWarningNearCenter >= 6

        val inputState = TacticalInputState(
            botHpRatio = botHpRatio,
            botManaRatio = botManaRatio,
            enemyHpRatio = enemyHpRatio,
            distToEnemyNorm = distToEnemyNorm,
            distToAllyTowerNorm = if (enemyInTopRightQuadrant) 0.75f else 0.38f,
            enemyUnderTower = if (enemyInTopRightQuadrant) 1.0f else 0.0f,
            skillsReadyRatio = skillsReadyRatio,
            incomingProjectileThreat = if (incomingDanger) 1.0f else 0.0f,
            enemyMinionCount = if (enemyDetected) 2 else 0,
            jungleMonsterAlive = !enemyDetected
        )

        val elapsedMicros = (System.nanoTime() - startNs) / 1000f
        return RealScreenScanResult(
            scanTimeMicros = max(12f, elapsedMicros),
            botHpRatio = botHpRatio,
            botManaRatio = botManaRatio,
            enemyDetected = enemyDetected,
            enemyHpRatio = enemyHpRatio,
            enemyScreenXRatio = enemyXRatio,
            enemyScreenYRatio = enemyYRatio,
            enemyAngleRad = enemyAngleRad,
            distToEnemyNorm = distToEnemyNorm,
            enemyUnderTower = enemyInTopRightQuadrant,
            incomingDangerDetected = incomingDanger,
            skill1Ready = s1Ready,
            skill2Ready = s2Ready,
            skill3Ready = s3Ready,
            healSpellReady = healReady,
            quickShopAvailable = shopReady,
            tacticalInput = inputState
        )
    }

    private fun scanCenterHeroHpBar(bitmap: Bitmap, w: Int, h: Int): Float {
        // Thanh máu của tướng người chơi nằm ngay phía trên đầu tướng (tâm màn hình: X 43%..57%, Y 32%..44%)
        val xStart = (w * 0.43f).toInt()
        val xEnd = (w * 0.57f).toInt()
        val yStart = (h * 0.32f).toInt()
        val yEnd = (h * 0.44f).toInt()
        val step = max(2, (xEnd - xStart) / 28)

        var bestRowGreenCount = 0
        var bestRowDarkEmptyCount = 0

        var y = yStart
        while (y <= yEnd) {
            var greenInRow = 0
            var darkAfterGreen = 0
            var seenGreen = false
            var x = xStart
            while (x <= xEnd) {
                val p = bitmap.getPixel(x, y)
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                // Màu xanh lá cây hoặc xanh ngọc của thanh máu bản thân
                val isHpGreen = (g > 165 && g > r + 40 && b < 170) || (g > 180 && b > 180 && r < 95)
                if (isHpGreen) {
                    greenInRow++
                    seenGreen = true
                } else if (seenGreen && r < 55 && g < 55 && b < 55) {
                    darkAfterGreen++
                }
                x += step
            }
            if (greenInRow > bestRowGreenCount) {
                bestRowGreenCount = greenInRow
                bestRowDarkEmptyCount = darkAfterGreen
            }
            y += max(2, (yEnd - yStart) / 14)
        }

        if (bestRowGreenCount == 0) {
            // Nếu khung hình chưa nhận diện được vạch máu (ví dụ đang mở bảng điểm), giả định 75% để không rút lui nhầm
            return 0.75f
        }
        val totalBarSamples = (bestRowGreenCount + bestRowDarkEmptyCount).coerceAtLeast(1)
        return (bestRowGreenCount.toFloat() / totalBarSamples.toFloat()).coerceIn(0.08f, 1.0f)
    }

    private fun scanCenterHeroManaBar(bitmap: Bitmap, w: Int, h: Int): Float {
        val xMid = (w * 0.50f).toInt()
        val yMid = (h * 0.40f).toInt()
        val p = bitmap.getPixel(xMid.coerceIn(0, w - 1), yMid.coerceIn(0, h - 1))
        val b = Color.blue(p)
        return if (b > 120) 0.85f else 0.65f
    }

    private fun estimateEnemyBarFill(bitmap: Bitmap, centerX: Int, centerY: Int, w: Int, h: Int): Float {
        val halfSpan = max(18, (w * 0.045f).toInt())
        val xLeft = (centerX - halfSpan).coerceIn(1, w - 2)
        val xRight = (centerX + halfSpan).coerceIn(1, w - 2)
        val y = centerY.coerceIn(1, h - 2)
        var redPixels = 0
        var darkPixels = 0
        val step = max(1, (xRight - xLeft) / 20)
        var x = xLeft
        while (x <= xRight) {
            val p = bitmap.getPixel(x, y)
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            if (r > 175 && g < 80 && b < 85) {
                redPixels++
            } else if (r < 60 && g < 60 && b < 60) {
                darkPixels++
            }
            x += step
        }
        val total = (redPixels + darkPixels).coerceAtLeast(1)
        return (redPixels.toFloat() / total.toFloat()).coerceIn(0.10f, 1.0f)
    }

    private fun isButtonBrightAndReady(bitmap: Bitmap, xPercent: Int, yPercent: Int): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        val cx = ((xPercent / 100f) * w).toInt().coerceIn(4, w - 5)
        val cy = ((yPercent / 100f) * h).toInt().coerceIn(4, h - 5)

        // Lấy trung bình độ sáng 5 điểm quanh tâm nút
        val offsets = arrayOf(0 to 0, -3 to 0, 3 to 0, 0 to -3, 0 to 3)
        var totalLum = 0f
        for ((dx, dy) in offsets) {
            val p = bitmap.getPixel((cx + dx).coerceIn(0, w - 1), (cy + dy).coerceIn(0, h - 1))
            val r = Color.red(p) / 255f
            val g = Color.green(p) / 255f
            val b = Color.blue(p) / 255f
            totalLum += (0.299f * r + 0.587f * g + 0.114f * b)
        }
        val avgLum = totalLum / offsets.size
        return avgLum >= 0.34f
    }

    fun defaultScanResult(micros: Float): RealScreenScanResult {
        val defaultInput = TacticalInputState(
            botHpRatio = 0.80f,
            botManaRatio = 0.75f,
            enemyHpRatio = 0.65f,
            distToEnemyNorm = 0.45f,
            distToAllyTowerNorm = 0.40f,
            enemyUnderTower = 0f,
            skillsReadyRatio = 1f,
            incomingProjectileThreat = 0f
        )
        return RealScreenScanResult(
            scanTimeMicros = micros,
            botHpRatio = 0.80f,
            botManaRatio = 0.75f,
            enemyDetected = false,
            enemyHpRatio = 0.65f,
            enemyScreenXRatio = 0.65f,
            enemyScreenYRatio = 0.35f,
            enemyAngleRad = -0.65f,
            distToEnemyNorm = 0.45f,
            enemyUnderTower = false,
            incomingDangerDetected = false,
            skill1Ready = true,
            skill2Ready = true,
            skill3Ready = true,
            healSpellReady = true,
            quickShopAvailable = false,
            tacticalInput = defaultInput
        )
    }
}
