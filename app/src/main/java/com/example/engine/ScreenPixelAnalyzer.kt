package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.viewmodel.TouchButtonCoord
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RealScreenScanResult(
    val scanTimeMicros: Float,
    val botHpRatio: Float,
    val botManaRatio: Float,
    val enemyDetected: Boolean,
    val enemyHpRatio: Float,
    val enemyScreenXRatio: Float,
    val enemyScreenYRatio: Float,
    val enemyAngleRad: Float,
    val distToEnemyNorm: Float,
    val enemyUnderTower: Boolean,
    val incomingDangerDetected: Boolean,
    val minimapVisibleEnemies: Int,
    val minimapMissingEnemiesMia: Int,
    val minimapAlliesNearby: Int,
    val groundFlowerDetected: Boolean,
    val skill1Ready: Boolean,
    val skill2Ready: Boolean,
    val skill3Ready: Boolean,
    val healSpellReady: Boolean,
    val recallButtonReady: Boolean,
    val quickShopAvailable: Boolean,
    val tacticalInput: TacticalInputState
)

/**
 * Bộ quét điểm ảnh màn hình Liên Quân thật tốc độ cao (< 2ms):
 * 1. Quét Bản Đồ Nhỏ (Minimap góc trái trên 2%..19% X, 3%..29% Y): Đếm địch lộ diện, địch mất tích (MIA), đồng minh.
 * 2. Quét Thanh máu xanh & Năng lượng của tướng bản thân ở tâm màn hình.
 * 3. Quét Thanh máu đỏ của tướng địch, lính địch & Hoa Florentino dưới mặt đất.
 * 4. Quét độ sáng (Luminance) các nút Chiêu 1, Chiêu 2, Chiêu 3, Biến Về, Cấp Cứu, Ô Mua Đồ Nhanh.
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

        // 1. Quét Bản Đồ Nhỏ (Minimap góc trái trên: X 2%..19%, Y 3%..29%)
        val (visibleEnemiesOnMap, alliesOnMap) = scanMinimapVision(bitmap, w, h)
        val miaCount = (5 - visibleEnemiesOnMap).coerceIn(0, 5)

        // 2. Quét thanh máu & mana nhân vật chính ở tâm màn hình
        val botHpRatio = scanCenterHeroHpBar(bitmap, w, h)
        val botManaRatio = scanCenterHeroManaBar(bitmap, w, h)

        // 3. Quét chiến trường tìm Thanh Máu Đỏ của Địch, Vòng Cảnh Báo Đạn & Hoa Florentino dưới đất
        var redCount = 0
        var sumEnemyX = 0f
        var sumEnemyY = 0f
        var brightWarningNearCenter = 0
        var flowerGoldCyanPixels = 0

        val stepX = max(4, w / 90)
        val stepY = max(4, h / 60)
        val startX = (w * 0.16f).toInt()
        val endX = (w * 0.88f).toInt()
        val startY = (h * 0.10f).toInt()
        val endY = (h * 0.80f).toInt()

        val centerX = w * 0.50f
        val centerY = h * 0.50f
        val dangerRadiusSq = (min(w, h) * 0.19f) * (min(w, h) * 0.19f)

        var y = startY
        while (y < endY) {
            var x = startX
            while (x < endX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Thanh máu đỏ của địch
                if (r > 190 && g < 72 && b < 78 && (r - g) > 125) {
                    redCount++
                    sumEnemyX += x
                    sumEnemyY += y
                }

                val dx = x - centerX
                val dy = y - centerY
                val distSq = dx * dx + dy * dy
                if (distSq <= dangerRadiusSq) {
                    // Vòng cảnh báo kỹ năng định hướng đỏ cam
                    if (r > 230 && g in 90..210 && b < 70) {
                        brightWarningNearCenter++
                    }
                    // Hoa Florentino / dấu ấn sáng vàng-trắng dưới mặt đất quanh tướng
                    if (r > 235 && g > 220 && b in 120..215) {
                        flowerGoldCyanPixels++
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

        val enemyHpRatio = if (enemyDetected) {
            estimateEnemyBarFill(bitmap, (enemyXRatio * w).toInt(), (enemyYRatio * h).toInt(), w, h)
        } else {
            0.85f
        }

        // 4. Quét độ sáng các nút điều khiển
        fun isCoordReady(id: String, defX: Int, defY: Int): Boolean {
            val c = coords.firstOrNull { it.id == id }
            return isButtonBrightAndReady(bitmap, c?.xPercent ?: defX, c?.yPercent ?: defY)
        }

        val s1Ready = isCoordReady("SKILL_1", 73, 78)
        val s2Ready = isCoordReady("SKILL_2", 78, 61)
        val s3Ready = isCoordReady("SKILL_3", 87, 50)
        val healReady = isCoordReady("SPELL_HEAL", 64, 86)
        val recallReady = isCoordReady("RECALL_BTN", 54, 86)
        val shopReady = isCoordReady("QUICK_SHOP", 11, 42)

        var readyCount = 0
        if (s1Ready) readyCount++
        if (s2Ready) readyCount++
        if (s3Ready) readyCount++
        val skillsReadyRatio = readyCount / 3f

        val enemyInTopRightQuadrant = enemyDetected && enemyXRatio > 0.65f && enemyYRatio < 0.35f
        val incomingDanger = brightWarningNearCenter >= 6
        val flowerFound = flowerGoldCyanPixels >= 4

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
            jungleMonsterAlive = !enemyDetected,
            missingEnemiesMia = miaCount,
            alliesNearbyCount = alliesOnMap,
            enemiesNearbyCount = if (enemyDetected) max(1, visibleEnemiesOnMap) else visibleEnemiesOnMap,
            groundFlowerDetected = flowerFound
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
            minimapVisibleEnemies = visibleEnemiesOnMap,
            minimapMissingEnemiesMia = miaCount,
            minimapAlliesNearby = alliesOnMap,
            groundFlowerDetected = flowerFound,
            skill1Ready = s1Ready,
            skill2Ready = s2Ready,
            skill3Ready = s3Ready,
            healSpellReady = healReady,
            recallButtonReady = recallReady,
            quickShopAvailable = shopReady,
            tacticalInput = inputState
        )
    }

    /**
     * Quét khung Bản Đồ Nhỏ (Minimap) góc trái trên màn hình để đếm chấm đỏ (địch) & chấm xanh (đồng minh)
     */
    private fun scanMinimapVision(bitmap: Bitmap, w: Int, h: Int): Pair<Int, Int> {
        val xStart = (w * 0.02f).toInt().coerceAtLeast(1)
        val xEnd = (w * 0.18f).toInt().coerceAtMost(w - 2)
        val yStart = (h * 0.03f).toInt().coerceAtLeast(1)
        val yEnd = (h * 0.29f).toInt().coerceAtMost(h - 2)
        val step = max(2, (xEnd - xStart) / 22)

        var redDots = 0
        var blueDots = 0
        var y = yStart
        while (y <= yEnd) {
            var x = xStart
            while (x <= xEnd) {
                val p = bitmap.getPixel(x, y)
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)
                if (r > 200 && g < 65 && b < 65) redDots++
                if (b > 195 && g > 140 && r < 80) blueDots++
                x += step
            }
            y += step
        }
        val estimatedEnemies = (redDots / 3).coerceIn(0, 5)
        val estimatedAllies = (blueDots / 3).coerceIn(1, 5)
        return estimatedEnemies to estimatedAllies
    }

    private fun scanCenterHeroHpBar(bitmap: Bitmap, w: Int, h: Int): Float {
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

        if (bestRowGreenCount == 0) return 0.78f
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
            botHpRatio = 0.82f,
            botManaRatio = 0.75f,
            enemyHpRatio = 0.60f,
            distToEnemyNorm = 0.42f,
            distToAllyTowerNorm = 0.40f,
            enemyUnderTower = 0f,
            skillsReadyRatio = 1f,
            incomingProjectileThreat = 0f,
            missingEnemiesMia = 1,
            alliesNearbyCount = 2,
            enemiesNearbyCount = 1,
            groundFlowerDetected = false
        )
        return RealScreenScanResult(
            scanTimeMicros = micros,
            botHpRatio = 0.82f,
            botManaRatio = 0.75f,
            enemyDetected = false,
            enemyHpRatio = 0.60f,
            enemyScreenXRatio = 0.65f,
            enemyScreenYRatio = 0.35f,
            enemyAngleRad = -0.65f,
            distToEnemyNorm = 0.42f,
            enemyUnderTower = false,
            incomingDangerDetected = false,
            minimapVisibleEnemies = 4,
            minimapMissingEnemiesMia = 1,
            minimapAlliesNearby = 2,
            groundFlowerDetected = false,
            skill1Ready = true,
            skill2Ready = true,
            skill3Ready = true,
            healSpellReady = true,
            recallButtonReady = true,
            quickShopAvailable = false,
            tacticalInput = defaultInput
        )
    }
}
