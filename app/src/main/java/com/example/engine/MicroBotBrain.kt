package com.example.engine

import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.data.TargetPriority
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

enum class BotTacticalAction(val code: Int, val titleVi: String, val badgeColorHex: Long) {
    ATTACK_COMBO(0, "Múa Combo Đặc Trưng Của Tướng", 0xFFFF5252),
    KITE_POKE(1, "Thả Diều Giữ Cự Ly & Cấu Rỉa", 0xFF00E5FF),
    PUSH_LANE(2, "Đi Đường Dọn Lính & Bấm Đẩy Trụ", 0xFFFFB300),
    FARM_JUNGLE(3, "Ăn Bùa Rừng / Tranh Chấp Rồng & Caesar", 0xFF00E676),
    RETREAT_HEAL(4, "Bị Gank Nguy Hiểm -> Tốc Biến / Cấp Cứu Lùi Trụ", 0xFFB388FF),
    DODGE_SKILLSHOT(5, "Lách 90° Né Chiêu Định Hướng", 0xFFFFEA00),
    SAFE_RECALL_BASE(6, "Lùi Vào Bụi/Trụ & Bấm Biến Về Hồi Máu/Mana", 0xFF40C4FF),
    ROAM_ROTATE(7, "Đọc Minimap Đảo Đường Hội Quân Hơn Người", 0xFF69F0AE)
}

data class TacticalInputState(
    val botHpRatio: Float,          // 0.0 .. 1.0
    val botManaRatio: Float,        // 0.0 .. 1.0
    val enemyHpRatio: Float,        // 0.0 .. 1.0
    val distToEnemyNorm: Float,     // 0.0 (sát) .. 1.0 (xa >= 600)
    val distToAllyTowerNorm: Float, // 0.0 .. 1.0
    val enemyUnderTower: Float,     // 1.0 nếu địch đứng trong tầm trụ địch, 0.0 nếu ngoài
    val skillsReadyRatio: Float,    // 0.0 .. 1.0 (tỷ lệ chiêu 1/2/3 sẵn sàng)
    val incomingProjectileThreat: Float, // 1.0 nếu có đạn chiêu định hướng bay tới
    val enemyMinionCount: Int = 2,
    val jungleMonsterAlive: Boolean = true,
    val missingEnemiesMia: Int = 1, // Số địch mất tích trên Minimap (0..5)
    val alliesNearbyCount: Int = 1, // Số đồng đội gần mình trên Minimap
    val enemiesNearbyCount: Int = 1,// Số địch gần mình trên Minimap
    val groundFlowerDetected: Boolean = false // Phát hiện Hoa Florentino / ấn nội tại dưới đất
) {
    fun toFloatArray8(): FloatArray = floatArrayOf(
        botHpRatio.coerceIn(0f, 1f),
        botManaRatio.coerceIn(0f, 1f),
        enemyHpRatio.coerceIn(0f, 1f),
        distToEnemyNorm.coerceIn(0f, 1f),
        (missingEnemiesMia / 5f).coerceIn(0f, 1f),
        enemyUnderTower.coerceIn(0f, 1f),
        skillsReadyRatio.coerceIn(0f, 1f),
        incomingProjectileThreat.coerceIn(0f, 1f)
    )
}

data class BrainDecisionResult(
    val action: BotTacticalAction,
    val reasonVi: String,
    val engineUsed: String,
    val executionMicros: Float,
    val actionProbabilities: FloatArray,
    val recommendedComboStep: String,
    val recommendedCounterItem: String = "Giày Kiên Cường + Thương Xuyên Phá",
    val combatWinConfidencePct: Int = 72
)

data class TrainingReport(
    val updatedWeights: FloatArray,
    val epochsRun: Int,
    val accuracyPercent: Float,
    val finalLoss: Float,
    val trainingDurationMs: Float
)

/**
 * Bộ não Chiến thuật Liên Quân Chuyên Sâu chạy 100% trên thiết bị (< 0.05ms):
 * Tích hợp đầy đủ:
 * 1. Đọc Minimap (Phát hiện địch mất tích MIA báo hiệu bị Gank, đếm quân số đồng minh vs địch)
 * 2. Khi nào nên Biến Về (Máu < ngưỡng rút lui hoặc Mana < ngưỡng cạn kiệt và đã lùi khỏi tầm địch)
 * 3. Khi nào nên Đánh / Nhịn (Tính xác suất thắng giao tranh dựa trên quân số, máu, chiêu cuối và trụ)
 * 4. Cách Múa Tướng riêng biệt (Florentino nhặt hoa 120°, Nakroth lướt ngược S2, Tulen tích 5 ấn, Hayate phi tiêu...)
 * 5. Tự động chọn Trang bị Khắc chế theo đội hình địch
 */
object MicroBotBrain {
    const val INPUT_SIZE = 8
    const val HIDDEN_SIZE = 6
    const val OUTPUT_SIZE = 5
    const val TOTAL_WEIGHTS = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE + (HIDDEN_SIZE * OUTPUT_SIZE) + OUTPUT_SIZE

    val INPUT_LABELS_VI = listOf(
        "Máu Bản Thân (%)",
        "Năng Lượng / Nội Năng (%)",
        "Máu Mục Tiêu Địch (%)",
        "Khoảng Cách Tới Địch",
        "Địch Mất Tích Trên Minimap (MIA)",
        "Địch Đang Ôm Trụ Bảo Vệ",
        "Tỷ Lệ Chiêu 1/2/Ulti Sẵn Sàng",
        "Đạn Định Hướng Đang Bay Tới"
    )

    val OUTPUT_ACTIONS = listOf(
        BotTacticalAction.ATTACK_COMBO,
        BotTacticalAction.KITE_POKE,
        BotTacticalAction.PUSH_LANE,
        BotTacticalAction.FARM_JUNGLE,
        BotTacticalAction.RETREAT_HEAL
    )

    fun decide(
        state: TacticalInputState,
        profile: BotProfileEntity,
        cachedWeights: FloatArray? = null
    ): BrainDecisionResult {
        val startNs = System.nanoTime()
        val mode = runCatching { BrainMode.valueOf(profile.brainMode) }.getOrDefault(BrainMode.HYBRID)
        val heroSpec = AovHeroAndMetaDatabase.getHeroSpecByIdOrRole(profile.selectedHeroId, profile.heroRole)
        val counterItem = pickSmartCounterItem(profile, heroSpec)
        val winConf = calculateCombatWinConfidence(state)

        val retreatRatio = profile.retreatHpPercent / 100f
        val recallManaRatio = profile.recallManaPercent / 100f
        val diveRatio = profile.diveTowerEnemyHpPercent / 100f

        // --- TẦNG CHIẾN THUẬT BẮT BUỘC (ÁP DỤNG CHO CẢ HYBRID & RULE_FSM) ---
        if (mode != BrainMode.MICRO_AI) {
            // 1. Né chiêu định hướng khẩn cấp (Vector 90 độ)
            if (state.incomingProjectileThreat > 0.6f && profile.autoDodgeSkillshot) {
                val us = max(0.8f, (System.nanoTime() - startNs) / 1000f)
                return BrainDecisionResult(
                    action = BotTacticalAction.DODGE_SKILLSHOT,
                    reasonVi = "Phát hiện vòng đỏ / đạn định hướng bay tới -> Kéo Joystick vuông góc 90° né đòn",
                    engineUsed = "Phản xạ Né Chiêu",
                    executionMicros = us,
                    actionProbabilities = floatArrayOf(0.05f, 0.80f, 0.05f, 0.05f, 0.05f),
                    recommendedComboStep = "Kéo Joystick 90° + Chiêu lướt",
                    recommendedCounterItem = counterItem,
                    combatWinConfidencePct = winConf
                )
            }

            // 2. Logic Biến Về Thông Minh (Khi máu thấp hoặc cạn Mana và đã giãn khoảng cách với địch)
            val needRecallHp = state.botHpRatio <= retreatRatio
            val needRecallMana = state.botManaRatio <= recallManaRatio && heroSpec.role == HeroRole.MAGE
            if (needRecallHp || needRecallMana) {
                val isEnemyTooClose = state.distToEnemyNorm < 0.36f
                val elapsedUs = max(0.9f, (System.nanoTime() - startNs) / 1000f)
                return if (isEnemyTooClose) {
                    // Địch đang đứng sát -> Phải Tốc Biến / Cấp Cứu chạy về trụ trước khi bấm Biến Về!
                    BrainDecisionResult(
                        action = BotTacticalAction.RETREAT_HEAL,
                        reasonVi = "Máu ${(state.botHpRatio * 100).toInt()}% (<= ${profile.retreatHpPercent}%) & Địch áp sát -> Bấm Cấp Cứu/Tốc Biến lùi về trụ!",
                        engineUsed = "Chiến thuật Thoát Giao Tranh",
                        executionMicros = elapsedUs,
                        actionProbabilities = floatArrayOf(0.02f, 0.06f, 0.02f, 0.02f, 0.88f),
                        recommendedComboStep = "Phép Bổ Trợ + Kéo Lùi 225°",
                        recommendedCounterItem = counterItem,
                        combatWinConfidencePct = winConf
                    )
                } else {
                    // Đã ở khoảng cách an toàn (trong trụ hoặc bụi cỏ) -> Bấm Nút Biến Về!
                    BrainDecisionResult(
                        action = BotTacticalAction.SAFE_RECALL_BASE,
                        reasonVi = "Máu ${(state.botHpRatio * 100).toInt()}% / Mana ${(state.botManaRatio * 100).toInt()}% thấp & đã vào vùng an toàn -> Bấm Biến Về hồi đầy máu!",
                        engineUsed = "Chiến thuật Biến Về Chuẩn",
                        executionMicros = elapsedUs,
                        actionProbabilities = floatArrayOf(0.02f, 0.05f, 0.03f, 0.05f, 0.85f),
                        recommendedComboStep = "Đứng Bụi/Trụ -> Bấm Biến Về (6s)",
                        recommendedCounterItem = counterItem,
                        combatWinConfidencePct = winConf
                    )
                }
            }

            // 3. Đọc Bản Đồ Nhỏ (Minimap MIA Anti-Gank & Quân số):
            // Nếu địch mất tích trên Minimap >= ngưỡng cảnh báo và mình đang đẩy cao một mình -> Lùi về giữ vị trí!
            if (state.missingEnemiesMia >= profile.minimapMiaAlertCount &&
                state.alliesNearbyCount < state.enemiesNearbyCount &&
                state.distToAllyTowerNorm > 0.55f
            ) {
                val elapsedUs = max(1.0f, (System.nanoTime() - startNs) / 1000f)
                return BrainDecisionResult(
                    action = BotTacticalAction.KITE_POKE,
                    reasonVi = "[Minimap Cảnh Báo] Có ${state.missingEnemiesMia} địch mất tích (MIA) & thua người (${state.alliesNearbyCount}v${state.enemiesNearbyCount}) -> Lùi giữ vị trí chống Gank!",
                    engineUsed = "AI Đọc Bản Đồ Nhỏ (Minimap)",
                    executionMicros = elapsedUs,
                    actionProbabilities = floatArrayOf(0.08f, 0.65f, 0.10f, 0.07f, 0.10f),
                    recommendedComboStep = "Kéo Lùi Ôm Mép Sông + Cấu Rỉa",
                    recommendedCounterItem = counterItem,
                    combatWinConfidencePct = winConf
                )
            }

            // 4. Chống Băng Trụ Ẩu khi địch còn nhiều máu dưới trụ
            if (state.enemyUnderTower > 0.5f && state.enemyHpRatio > diveRatio && state.distToEnemyNorm < 0.45f) {
                val elapsedUs = max(1.0f, (System.nanoTime() - startNs) / 1000f)
                return BrainDecisionResult(
                    action = BotTacticalAction.PUSH_LANE,
                    reasonVi = "Địch ôm trụ còn ${(state.enemyHpRatio * 100).toInt()}% máu (> ngưỡng băng trụ ${profile.diveTowerEnemyHpPercent}%) -> Dọn lính cấu máu trụ, không lao vào trụ",
                    engineUsed = "Luật Chống Băng Trụ",
                    executionMicros = elapsedUs,
                    actionProbabilities = floatArrayOf(0.05f, 0.25f, 0.58f, 0.07f, 0.05f),
                    recommendedComboStep = "Bấm Nút Đánh Lính / Đẩy Trụ",
                    recommendedCounterItem = counterItem,
                    combatWinConfidencePct = winConf
                )
            }
        }

        // --- TẦNG MÚA TƯỚNG & GIAO TRANH (DÙNG MICRO-MLP HOẶC LUẬT CHUYÊN SÂU) ---
        val weights = cachedWeights ?: parseWeightsOrDefault(profile)
        val probs = forwardMicroMlp(state.toFloatArray8(), weights)

        // Cộng điểm ưu tiên nếu phát hiện Hoa Florentino dưới đất hoặc lợi thế thắng giao tranh cao
        if (state.groundFlowerDetected && heroSpec.id == "FLORENTINO") {
            probs[0] += 0.35f
        }
        if (winConf >= 68 && state.distToEnemyNorm < 0.55f) {
            probs[0] += 0.22f
        } else if (winConf < 45 && state.distToEnemyNorm < 0.50f) {
            probs[1] += 0.25f
        }
        normalizeProbabilities(probs)

        var bestIdx = 0
        for (i in 1 until OUTPUT_SIZE) {
            if (probs[i] > probs[bestIdx]) bestIdx = i
        }

        var chosenAction = OUTPUT_ACTIONS[bestIdx]
        // Nếu đường đã sạch lính và đồng đội đang giao tranh gần đó trên Minimap -> Đảo đường hội quân!
        if (chosenAction == BotTacticalAction.PUSH_LANE && state.enemyMinionCount == 0 && state.alliesNearbyCount >= 2) {
            chosenAction = BotTacticalAction.ROAM_ROTATE
        }

        val comboPreview = heroSpec.signatureComboSteps.take(4).joinToString(" → ") { it.stepTitle }
        val elapsedMicros = max(1.2f, (System.nanoTime() - startNs) / 1000f)

        val reasonText = when (chosenAction) {
            BotTacticalAction.ATTACK_COMBO ->
                "Lợi thế giao tranh $winConf% -> Kích hoạt Múa ${heroSpec.heroName.substringBefore(" ")}: ${heroSpec.mechanicsSummaryVi.substringBefore(".")}"
            BotTacticalAction.KITE_POKE ->
                "Giữ khoảng cách thả diều ${profile.kitingDistance}px (${heroSpec.heroName.substringBefore(" ")}) vừa lùi vừa cấu rỉa"
            BotTacticalAction.ROAM_ROTATE ->
                "Đã dọn xong lính đường ${profile.assignedLane} -> Nhìn Minimap đảo đường hội quân đánh hơn người"
            BotTacticalAction.FARM_JUNGLE ->
                "Đường an toàn -> Xoay vào ăn Bùa / Quái rừng & kiểm soát mục tiêu lớn"
            else ->
                "Đi đường ${profile.assignedLane} dọn lính lên cấp & ép máu trụ địch"
        }

        return BrainDecisionResult(
            action = chosenAction,
            reasonVi = reasonText,
            engineUsed = "${mode.titleVi.substringBefore(" (")} • ${heroSpec.id}",
            executionMicros = elapsedMicros,
            actionProbabilities = probs,
            recommendedComboStep = comboPreview,
            recommendedCounterItem = counterItem,
            combatWinConfidencePct = winConf
        )
    }

    /**
     * Tính toán tỷ lệ % thắng giao tranh (Should Fight Evaluator) từ 0..99%
     */
    fun calculateCombatWinConfidence(state: TacticalInputState): Int {
        var score = 50f
        // Chênh lệch máu
        score += (state.botHpRatio - state.enemyHpRatio) * 38f
        // Sẵn sàng bộ chiêu thức
        score += (state.skillsReadyRatio - 0.4f) * 25f
        // Chênh lệch quân số trên Minimap
        score += (state.alliesNearbyCount - state.enemiesNearbyCount) * 16f
        // Trừ điểm nếu địch đứng trong trụ của chúng
        if (state.enemyUnderTower > 0.5f && state.enemyHpRatio > 0.22f) {
            score -= 26f
        }
        // Trừ điểm nếu nhiều địch mất tích (MIA)
        if (state.missingEnemiesMia >= 3) {
            score -= 14f
        }
        return score.toInt().coerceIn(5, 98)
    }

    private fun pickSmartCounterItem(profile: BotProfileEntity, heroSpec: HeroMechanicsSpec): String {
        val tags = profile.enemyCompTagsCsv
        return when {
            tags.contains("HEAL_TANK") ->
                if (heroSpec.role == HeroRole.MAGE) "Sách Truy Hồn (Khắc chế Hồi máu)" else "Đao Truy Hồn (Khắc chế Hồi máu)"
            tags.contains("HARD_CC") ->
                "Giày Kiên Cường + Quả Cầu Băng Sương / Thanh Tẩy"
            tags.contains("ASSASSIN_BURST") ->
                "Khiên Thất Truyền + Giáp Hộ Mệnh (Đổi Liềm Đoạt Mệnh)"
            tags.contains("POKE_MAGE") ->
                "Huân Chương Troy (Kháng phép cao)"
            else ->
                heroSpec.coreItems.take(3).joinToString(" + ")
        }
    }

    fun forwardMicroMlp(inputs: FloatArray, weights: FloatArray): FloatArray {
        val w = if (weights.size == TOTAL_WEIGHTS) weights else createPresetWeights(HeroRole.MARKSMAN)
        val hidden = FloatArray(HIDDEN_SIZE)
        var offset = 0

        for (h in 0 until HIDDEN_SIZE) {
            var sum = 0f
            for (i in 0 until INPUT_SIZE) {
                sum += inputs[i] * w[offset++]
            }
            hidden[h] = sum
        }
        for (h in 0 until HIDDEN_SIZE) {
            val biased = hidden[h] + w[offset++]
            hidden[h] = if (biased > 0f) biased else 0.05f * biased
        }

        val logits = FloatArray(OUTPUT_SIZE)
        for (o in 0 until OUTPUT_SIZE) {
            var sum = 0f
            for (h in 0 until HIDDEN_SIZE) {
                sum += hidden[h] * w[offset++]
            }
            logits[o] = sum
        }
        var maxLogit = Float.NEGATIVE_INFINITY
        for (o in 0 until OUTPUT_SIZE) {
            logits[o] += w[offset++]
            if (logits[o] > maxLogit) maxLogit = logits[o]
        }

        var sumExp = 0f
        val probs = FloatArray(OUTPUT_SIZE)
        for (o in 0 until OUTPUT_SIZE) {
            val e = exp((logits[o] - maxLogit).coerceIn(-12f, 12f))
            probs[o] = e
            sumExp += e
        }
        if (sumExp > 0f) {
            for (o in 0 until OUTPUT_SIZE) {
                probs[o] /= sumExp
            }
        }
        return probs
    }

    fun trainOnDevice(
        initialWeights: FloatArray,
        role: HeroRole,
        retreatHpPercent: Int,
        epochs: Int,
        learningRate: Float = 0.045f
    ): TrainingReport {
        val startNs = System.nanoTime()
        val w = if (initialWeights.size == TOTAL_WEIGHTS) initialWeights.copyOf() else createPresetWeights(role)
        val samples = buildTacticalTrainingSamples(role, retreatHpPercent / 100f)

        var totalLoss = 0f
        var correctCount = 0

        val hiddenPre = FloatArray(HIDDEN_SIZE)
        val hiddenAct = FloatArray(HIDDEN_SIZE)
        val logits = FloatArray(OUTPUT_SIZE)
        val probs = FloatArray(OUTPUT_SIZE)
        val dOut = FloatArray(OUTPUT_SIZE)
        val dHidden = FloatArray(HIDDEN_SIZE)

        val b1Offset = INPUT_SIZE * HIDDEN_SIZE
        val w2Offset = b1Offset + HIDDEN_SIZE
        val b2Offset = w2Offset + (HIDDEN_SIZE * OUTPUT_SIZE)

        for (epoch in 0 until epochs) {
            totalLoss = 0f
            correctCount = 0

            for (sample in samples) {
                val x = sample.first
                val target = sample.second

                var idx = 0
                for (h in 0 until HIDDEN_SIZE) {
                    var sum = 0f
                    for (i in 0 until INPUT_SIZE) {
                        sum += x[i] * w[idx++]
                    }
                    val pre = sum + w[b1Offset + h]
                    hiddenPre[h] = pre
                    hiddenAct[h] = if (pre > 0f) pre else 0.05f * pre
                }

                var maxLogit = Float.NEGATIVE_INFINITY
                idx = w2Offset
                for (o in 0 until OUTPUT_SIZE) {
                    var sum = 0f
                    for (h in 0 until HIDDEN_SIZE) {
                        sum += hiddenAct[h] * w[idx++]
                    }
                    val l = sum + w[b2Offset + o]
                    logits[o] = l
                    if (l > maxLogit) maxLogit = l
                }

                var sumExp = 0f
                for (o in 0 until OUTPUT_SIZE) {
                    val e = exp((logits[o] - maxLogit).coerceIn(-12f, 12f))
                    probs[o] = e
                    sumExp += e
                }
                var predBest = 0
                for (o in 0 until OUTPUT_SIZE) {
                    probs[o] /= sumExp
                    if (probs[o] > probs[predBest]) predBest = o
                }
                if (predBest == target) correctCount++

                val targetProb = probs[target].coerceAtLeast(1e-5f)
                totalLoss += -kotlin.math.ln(targetProb)

                for (o in 0 until OUTPUT_SIZE) {
                    dOut[o] = probs[o] - (if (o == target) 1f else 0f)
                }

                for (h in 0 until HIDDEN_SIZE) {
                    var gradH = 0f
                    for (o in 0 until OUTPUT_SIZE) {
                        gradH += dOut[o] * w[w2Offset + o * HIDDEN_SIZE + h]
                    }
                    val reluDeriv = if (hiddenPre[h] > 0f) 1f else 0.05f
                    dHidden[h] = gradH * reluDeriv
                }

                for (o in 0 until OUTPUT_SIZE) {
                    for (h in 0 until HIDDEN_SIZE) {
                        val w2Idx = w2Offset + o * HIDDEN_SIZE + h
                        w[w2Idx] -= learningRate * dOut[o] * hiddenAct[h]
                    }
                    w[b2Offset + o] -= learningRate * dOut[o]
                }

                for (h in 0 until HIDDEN_SIZE) {
                    for (i in 0 until INPUT_SIZE) {
                        val w1Idx = h * INPUT_SIZE + i
                        w[w1Idx] -= learningRate * dHidden[h] * x[i]
                    }
                    w[b1Offset + h] -= learningRate * dHidden[h]
                }
            }
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000f
        val acc = (correctCount.toFloat() / samples.size.toFloat()) * 100f
        val avgLoss = totalLoss / samples.size.toFloat()
        return TrainingReport(
            updatedWeights = w,
            epochsRun = epochs,
            accuracyPercent = acc.coerceIn(0f, 100f),
            finalLoss = avgLoss,
            trainingDurationMs = max(0.1f, durationMs)
        )
    }

    private fun buildTacticalTrainingSamples(role: HeroRole, retreatRatio: Float): List<Pair<FloatArray, Int>> {
        val list = ArrayList<Pair<FloatArray, Int>>(45)
        // [botHp, botMana, enemyHp, distEnemy, miaRatio, enemyUnderTower, skillsReady, projectileThreat]
        // Nhãn 4: RETREAT_HEAL / BIẾN VỀ khi máu thấp hoặc bị vây
        list.add(floatArrayOf(retreatRatio * 0.6f, 0.5f, 0.8f, 0.3f, 0.6f, 0f, 0.5f, 0f) to 4)
        list.add(floatArrayOf(retreatRatio * 0.85f, 0.2f, 0.9f, 0.25f, 0.8f, 0f, 0.2f, 0.8f) to 4)
        list.add(floatArrayOf(0.15f, 0.1f, 0.6f, 0.4f, 0.4f, 1f, 0.3f, 0f) to 4)
        list.add(floatArrayOf(0.18f, 0.7f, 0.75f, 0.2f, 0.8f, 0f, 0.4f, 1f) to 4)

        // Nhãn 0: ATTACK_COMBO (Múa tướng kết liễu khi có chiêu & lợi thế máu)
        list.add(floatArrayOf(0.85f, 0.8f, 0.22f, 0.25f, 0.2f, 0f, 0.9f, 0f) to 0)
        list.add(floatArrayOf(0.75f, 0.6f, 0.15f, 0.2f, 0.2f, 1f, 1.0f, 0f) to 0)
        list.add(floatArrayOf(0.90f, 0.7f, 0.35f, 0.3f, 0.2f, 0f, 0.85f, 0f) to 0)
        if (role == HeroRole.ASSASSIN || role == HeroRole.WARRIOR) {
            list.add(floatArrayOf(0.80f, 0.7f, 0.55f, 0.22f, 0.2f, 0f, 0.9f, 0f) to 0)
            list.add(floatArrayOf(0.75f, 0.6f, 0.48f, 0.18f, 0.2f, 0f, 0.8f, 0f) to 0)
        }

        // Nhãn 1: KITE_POKE khi địch MIA cao hoặc cần giữ khoảng cách cấu rỉa
        list.add(floatArrayOf(0.75f, 0.7f, 0.70f, 0.42f, 0.6f, 0f, 0.6f, 0f) to 1)
        list.add(floatArrayOf(0.65f, 0.6f, 0.65f, 0.35f, 0.8f, 0f, 0.5f, 0.4f) to 1)
        if (role == HeroRole.MARKSMAN || role == HeroRole.MAGE) {
            list.add(floatArrayOf(0.80f, 0.75f, 0.55f, 0.30f, 0.4f, 0f, 0.7f, 0f) to 1)
            list.add(floatArrayOf(0.60f, 0.5f, 0.80f, 0.28f, 0.6f, 0f, 0.4f, 0f) to 1)
        }

        // Nhãn 2: PUSH_LANE khi địch ở xa hoặc địch ôm trụ đầy máu
        list.add(floatArrayOf(0.85f, 0.7f, 0.85f, 0.85f, 0.2f, 1f, 0.5f, 0f) to 2)
        list.add(floatArrayOf(0.75f, 0.6f, 0.70f, 0.75f, 0.2f, 1f, 0.4f, 0f) to 2)
        list.add(floatArrayOf(0.90f, 0.8f, 0.90f, 0.90f, 0.0f, 0f, 0.6f, 0f) to 2)

        // Nhãn 3: FARM_JUNGLE khi đang chờ hồi chiêu & ăn mục tiêu
        list.add(floatArrayOf(0.70f, 0.5f, 0.95f, 0.92f, 0.4f, 1f, 0.2f, 0f) to 3)
        list.add(floatArrayOf(0.65f, 0.4f, 0.85f, 0.80f, 0.2f, 0f, 0.15f, 0f) to 3)

        return list
    }

    fun createPresetWeights(role: HeroRole): FloatArray {
        val rng = Random(role.ordinal * 1337 + 42)
        val raw = FloatArray(TOTAL_WEIGHTS) { (rng.nextFloat() - 0.5f) * 0.6f }
        return trainOnDevice(raw, role, retreatHpPercent = 26, epochs = 200, learningRate = 0.05f).updatedWeights
    }

    fun weightsToCsv(weights: FloatArray): String =
        weights.joinToString(",") { "%.4f".format(java.util.Locale.US, it) }

    fun parseWeightsOrDefault(profile: BotProfileEntity): FloatArray {
        if (profile.weightsCsv.isBlank()) {
            val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
            return createPresetWeights(role)
        }
        val parsed = profile.weightsCsv.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .toFloatArray()
        return if (parsed.size == TOTAL_WEIGHTS) {
            parsed
        } else {
            val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
            createPresetWeights(role)
        }
    }

    private fun normalizeProbabilities(probs: FloatArray) {
        var sum = 0f
        for (p in probs) sum += max(0f, p)
        if (sum > 0f) {
            for (i in probs.indices) probs[i] = max(0f, probs[i]) / sum
        }
    }

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
