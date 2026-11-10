package com.example.engine

import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.data.TargetPriority
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

enum class BotTacticalAction(val code: Int, val titleVi: String, val badgeColorHex: Long) {
    ATTACK_COMBO(0, "Dồn Combo Tấn Công Địch", 0xFFFF5252),
    KITE_POKE(1, "Thả Diều & Cấu Rỉa Giữ Vị Trí", 0xFF00E5FF),
    PUSH_LANE(2, "Dọn Lính & Đẩy Trụ Đường Giữa", 0xFFFFB300),
    FARM_JUNGLE(3, "Ăn Quái Rừng / Bùa Tăng Tốc", 0xFF00E676),
    RETREAT_HEAL(4, "Rút Lui Về Trụ & Hồi Phục", 0xFFB388FF),
    DODGE_SKILLSHOT(5, "Lách Né Chiêu Định Hướng (Vector 90°)", 0xFFFFEA00)
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
    val jungleMonsterAlive: Boolean = true
) {
    fun toFloatArray8(): FloatArray = floatArrayOf(
        botHpRatio.coerceIn(0f, 1f),
        botManaRatio.coerceIn(0f, 1f),
        enemyHpRatio.coerceIn(0f, 1f),
        distToEnemyNorm.coerceIn(0f, 1f),
        distToAllyTowerNorm.coerceIn(0f, 1f),
        enemyUnderTower.coerceIn(0f, 1f),
        skillsReadyRatio.coerceIn(0f, 1f),
        incomingProjectileThreat.coerceIn(0f, 1f)
    )
}

data class BrainDecisionResult(
    val action: BotTacticalAction,
    val reasonVi: String,
    val engineUsed: String,
    val executionMicros: Float, // Thời gian thực đo bằng System.nanoTime() / 1000f
    val actionProbabilities: FloatArray, // 5 xác suất cho 5 hành động chính
    val recommendedComboStep: String
)

data class TrainingReport(
    val updatedWeights: FloatArray,
    val epochsRun: Int,
    val accuracyPercent: Float,
    val finalLoss: Float,
    val trainingDurationMs: Float
)

/**
 * Bộ não xử lý siêu tốc chạy 100% trên thiết bị (On-Device):
 * - Chế độ 1: Luật lập trình sẵn (FSM / Cây quyết định) -> 0 tham số AI, tốc độ ~0.003ms
 * - Chế độ 2: Mạng nơ-ron siêu nhẹ tự tạo (Micro-MLP: 8 đầu vào -> 6 nút ẩn ReLU -> 5 đầu ra) = 89 trọng số Float
 * - Chế độ 3: Hybrid (Dùng luật cứng cho phản xạ sinh tồn + Micro-MLP cho giao tranh)
 */
object MicroBotBrain {
    const val INPUT_SIZE = 8
    const val HIDDEN_SIZE = 6
    const val OUTPUT_SIZE = 5
    // Tổng số trọng số: (8 * 6) + 6 (bias ẩn) + (6 * 5) + 5 (bias ra) = 48 + 6 + 30 + 5 = 89 floats
    const val TOTAL_WEIGHTS = (INPUT_SIZE * HIDDEN_SIZE) + HIDDEN_SIZE + (HIDDEN_SIZE * OUTPUT_SIZE) + OUTPUT_SIZE

    val INPUT_LABELS_VI = listOf(
        "Máu Bot (%)",
        "Năng lượng (%)",
        "Máu Địch (%)",
        "Khoảng cách tới Địch",
        "Khoảng cách tới Trụ nhà",
        "Địch đang ôm Trụ",
        "Chiêu thức Sẵn sàng",
        "Đạn định hướng bay tới"
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
        val comboSteps = profile.comboSequence.split("->").map { it.trim() }.filter { it.isNotEmpty() }
        val primaryCombo = comboSteps.firstOrNull() ?: "S1"

        return when (mode) {
            BrainMode.RULE_FSM -> {
                val (action, reason, probs) = evaluateRuleFsm(state, profile)
                val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                BrainDecisionResult(
                    action = action,
                    reasonVi = reason,
                    engineUsed = "FSM Lập trình sẵn",
                    executionMicros = max(0.8f, elapsedMicros),
                    actionProbabilities = probs,
                    recommendedComboStep = pickComboStep(state, comboSteps)
                )
            }

            BrainMode.MICRO_AI -> {
                val weights = cachedWeights ?: parseWeightsOrDefault(profile)
                val probs = forwardMicroMlp(state.toFloatArray8(), weights)
                var bestIdx = 0
                for (i in 1 until OUTPUT_SIZE) {
                    if (probs[i] > probs[bestIdx]) bestIdx = i
                }
                val chosenAction = if (state.incomingProjectileThreat > 0.65f && profile.autoDodgeSkillshot) {
                    BotTacticalAction.DODGE_SKILLSHOT
                } else {
                    OUTPUT_ACTIONS[bestIdx]
                }
                val confPct = (probs[bestIdx] * 100f).toInt()
                val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                BrainDecisionResult(
                    action = chosenAction,
                    reasonVi = if (chosenAction == BotTacticalAction.DODGE_SKILLSHOT) {
                        "Phát hiện chiêu định hướng! Lách vector 90° trước khi chạy MLP ($confPct%)"
                    } else {
                        "Micro-MLP (89 tham số) chấm điểm cao nhất: ${chosenAction.titleVi} ($confPct%)"
                    },
                    engineUsed = "Micro-MLP Nội Bộ",
                    executionMicros = max(1.2f, elapsedMicros),
                    actionProbabilities = probs,
                    recommendedComboStep = pickComboStep(state, comboSteps)
                )
            }

            BrainMode.HYBRID -> {
                val retreatThreshold = profile.retreatHpPercent / 100f
                val diveThreshold = profile.diveTowerEnemyHpPercent / 100f

                // Tầng 1: Phản xạ cứng tức thì (0ms) cho tình huống sinh tử
                if (state.incomingProjectileThreat > 0.6f && profile.autoDodgeSkillshot) {
                    val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                    return BrainDecisionResult(
                        action = BotTacticalAction.DODGE_SKILLSHOT,
                        reasonVi = "[Luật Cứng] Đạn địch áp sát -> Lách vuông góc 90° tức thì",
                        engineUsed = "Hybrid (Phản xạ Luật)",
                        executionMicros = max(0.9f, elapsedMicros),
                        actionProbabilities = floatArrayOf(0.05f, 0.75f, 0.05f, 0.05f, 0.10f),
                        recommendedComboStep = "Lách 90° + Tốc Biến"
                    )
                }

                if (state.botHpRatio <= retreatThreshold) {
                    val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                    return BrainDecisionResult(
                        action = BotTacticalAction.RETREAT_HEAL,
                        reasonVi = "[Luật Cứng] Máu ${(state.botHpRatio * 100).toInt()}% <= ngưỡng rút lui ${profile.retreatHpPercent}%",
                        engineUsed = "Hybrid (Bảo toàn mạng)",
                        executionMicros = max(0.9f, elapsedMicros),
                        actionProbabilities = floatArrayOf(0.02f, 0.08f, 0.03f, 0.02f, 0.85f),
                        recommendedComboStep = "Hồi Máu + Lùi Trụ"
                    )
                }

                if (state.enemyUnderTower > 0.5f && state.enemyHpRatio > diveThreshold && state.distToEnemyNorm < 0.45f) {
                    val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                    return BrainDecisionResult(
                        action = BotTacticalAction.PUSH_LANE,
                        reasonVi = "[Luật Cứng] Địch ôm trụ còn ${(state.enemyHpRatio * 100).toInt()}% máu (> ${profile.diveTowerEnemyHpPercent}%) -> Không băng trụ ẩu",
                        engineUsed = "Hybrid (Chống băng trụ)",
                        executionMicros = max(1.0f, elapsedMicros),
                        actionProbabilities = floatArrayOf(0.05f, 0.25f, 0.55f, 0.10f, 0.05f),
                        recommendedComboStep = primaryCombo
                    )
                }

                // Tầng 2: Tình huống giao tranh mở -> Dùng Micro-MLP kết hợp trọng số ưu tiên của người dùng
                val weights = cachedWeights ?: parseWeightsOrDefault(profile)
                val probs = forwardMicroMlp(state.toFloatArray8(), weights)

                // Điều chỉnh nhẹ theo TargetPriority người dùng chọn
                val priority = runCatching { TargetPriority.valueOf(profile.targetPriority) }
                    .getOrDefault(TargetPriority.LOWEST_HP_HERO)
                when (priority) {
                    TargetPriority.LOWEST_HP_HERO -> if (state.enemyHpRatio < 0.45f) probs[0] += 0.22f
                    TargetPriority.NEAREST_ENEMY -> probs[1] += 0.18f
                    TargetPriority.TOWER_AND_WAVE -> if (state.enemyMinionCount > 0) probs[2] += 0.24f
                    TargetPriority.JUNGLE_FARM -> if (state.jungleMonsterAlive) probs[3] += 0.25f
                }
                normalizeProbabilities(probs)

                var bestIdx = 0
                for (i in 1 until OUTPUT_SIZE) {
                    if (probs[i] > probs[bestIdx]) bestIdx = i
                }
                val chosenAction = OUTPUT_ACTIONS[bestIdx]
                val confPct = (probs[bestIdx] * 100f).toInt()
                val elapsedMicros = (System.nanoTime() - startNs) / 1000f
                BrainDecisionResult(
                    action = chosenAction,
                    reasonVi = "[Hybrid AI+Luật] Chọn ${chosenAction.titleVi} (Độ tin cậy $confPct%)",
                    engineUsed = "Hybrid (FSM + Micro-MLP)",
                    executionMicros = max(1.4f, elapsedMicros),
                    actionProbabilities = probs,
                    recommendedComboStep = pickComboStep(state, comboSteps)
                )
            }
        }
    }

    private fun pickComboStep(state: TacticalInputState, comboSteps: List<String>): String {
        if (comboSteps.isEmpty()) return "AA"
        return when {
            state.skillsReadyRatio > 0.8f -> comboSteps.joinToString(" → ")
            state.skillsReadyRatio > 0.45f -> comboSteps.take(2).joinToString(" → ")
            else -> "Đánh thường (AA) + Giữ vị trí"
        }
    }

    private fun evaluateRuleFsm(
        state: TacticalInputState,
        profile: BotProfileEntity
    ): Triple<BotTacticalAction, String, FloatArray> {
        val retreatRatio = profile.retreatHpPercent / 100f
        val diveRatio = profile.diveTowerEnemyHpPercent / 100f
        val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)

        // Luật 1: Né chiêu định hướng
        if (state.incomingProjectileThreat > 0.6f && profile.autoDodgeSkillshot) {
            return Triple(
                BotTacticalAction.DODGE_SKILLSHOT,
                "Luật #1: Phát hiện đường đạn địch -> Tách góc 90° né chiêu",
                floatArrayOf(0.05f, 0.80f, 0.05f, 0.05f, 0.05f)
            )
        }

        // Luật 2: Máu thấp rút lui về trụ
        if (state.botHpRatio <= retreatRatio) {
            return Triple(
                BotTacticalAction.RETREAT_HEAL,
                "Luật #2: Máu Bot ${(state.botHpRatio * 100).toInt()}% chạm ngưỡng rút lui (${profile.retreatHpPercent}%)",
                floatArrayOf(0.02f, 0.05f, 0.03f, 0.02f, 0.88f)
            )
        }

        // Luật 3: Kết liễu địch thấp máu (nếu không ôm trụ hoặc máu địch dưới ngưỡng băng trụ)
        val canDive = state.enemyUnderTower < 0.5f || state.enemyHpRatio <= diveRatio
        if (state.enemyHpRatio < 0.38f && canDive && state.skillsReadyRatio >= 0.33f) {
            return Triple(
                BotTacticalAction.ATTACK_COMBO,
                "Luật #3: Địch yếu máu (${(state.enemyHpRatio * 100).toInt()}%) -> Dồn chuỗi ${profile.comboSequence}",
                floatArrayOf(0.82f, 0.10f, 0.04f, 0.02f, 0.02f)
            )
        }

        // Luật 4: Theo chất tướng (Xạ thủ/Pháp sư thả diều; Sát thủ/Đấu sĩ áp sát khi đủ chiêu)
        if (state.distToEnemyNorm < 0.55f && state.enemyUnderTower < 0.5f) {
            return if (role == HeroRole.MARKSMAN || role == HeroRole.MAGE) {
                Triple(
                    BotTacticalAction.KITE_POKE,
                    "Luật #4: Giữ tầm thả diều ${profile.kitingDistance}px (${role.displayName}) vừa lùi vừa bắn",
                    floatArrayOf(0.20f, 0.68f, 0.07f, 0.03f, 0.02f)
                )
            } else {
                Triple(
                    BotTacticalAction.ATTACK_COMBO,
                    "Luật #4: Áp sát mục tiêu (${role.displayName}) kích hoạt combo",
                    floatArrayOf(0.72f, 0.16f, 0.07f, 0.03f, 0.02f)
                )
            }
        }

        // Luật 5: Dọn lính hoặc ăn rừng khi đường trống
        if (state.enemyMinionCount == 0 && profile.autoJungleWhenLaneClear && state.jungleMonsterAlive) {
            return Triple(
                BotTacticalAction.FARM_JUNGLE,
                "Luật #5: Đợt lính đã dọn sạch -> Xoay sang ăn Bùa/Quái rừng tối ưu vàng",
                floatArrayOf(0.05f, 0.10f, 0.12f, 0.70f, 0.03f)
            )
        }

        return Triple(
            BotTacticalAction.PUSH_LANE,
            "Luật #6: Dọn lính đường giữa và cấu máu trụ địch an toàn",
            floatArrayOf(0.10f, 0.18f, 0.58f, 0.10f, 0.04f)
        )
    }

    /**
     * Suy luận mạng nơ-ron 2 tầng (8 -> 6 ReLU -> 5 Softmax) bằng mảng tĩnh tốc độ cao
     */
    fun forwardMicroMlp(inputs: FloatArray, weights: FloatArray): FloatArray {
        val w = if (weights.size == TOTAL_WEIGHTS) weights else createPresetWeights(HeroRole.MARKSMAN)
        val hidden = FloatArray(HIDDEN_SIZE)
        var offset = 0

        // Tầng 1: 8 inputs -> 6 hidden (ReLU)
        for (h in 0 until HIDDEN_SIZE) {
            var sum = 0f
            for (i in 0 until INPUT_SIZE) {
                sum += inputs[i] * w[offset++]
            }
            hidden[h] = sum
        }
        for (h in 0 until HIDDEN_SIZE) {
            val biased = hidden[h] + w[offset++]
            hidden[h] = if (biased > 0f) biased else 0.05f * biased // Leaky ReLU
        }

        // Tầng 2: 6 hidden -> 5 outputs
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

        // Softmax
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

    /**
     * Huấn luyện trực tiếp mạng Micro-MLP ngay trên CPU điện thoại (Backpropagation nhẹ trên tập mẫu chiến thuật Liên Quân)
     */
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

                // Forward
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

                // Backward (Cross-Entropy + Softmax gradient)
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

                // Cập nhật W2, b2
                for (o in 0 until OUTPUT_SIZE) {
                    for (h in 0 until HIDDEN_SIZE) {
                        val w2Idx = w2Offset + o * HIDDEN_SIZE + h
                        w[w2Idx] -= learningRate * dOut[o] * hiddenAct[h]
                    }
                    w[b2Offset + o] -= learningRate * dOut[o]
                }

                // Cập nhật W1, b1
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
        val list = ArrayList<Pair<FloatArray, Int>>(40)
        // [botHp, botMana, enemyHp, distEnemy, distAllyTower, enemyUnderTower, skillsReady, projectileThreat]
        // Nhãn 4: RETREAT_HEAL khi máu thấp
        list.add(floatArrayOf(retreatRatio * 0.6f, 0.5f, 0.8f, 0.3f, 0.6f, 0f, 0.5f, 0f) to 4)
        list.add(floatArrayOf(retreatRatio * 0.85f, 0.2f, 0.9f, 0.25f, 0.7f, 0f, 0.2f, 0.8f) to 4)
        list.add(floatArrayOf(0.15f, 0.1f, 0.6f, 0.4f, 0.5f, 1f, 0.3f, 0f) to 4)
        list.add(floatArrayOf(0.18f, 0.7f, 0.75f, 0.2f, 0.8f, 0f, 0.4f, 1f) to 4)

        // Nhãn 0: ATTACK_COMBO khi địch thấp máu và đủ chiêu
        list.add(floatArrayOf(0.85f, 0.8f, 0.22f, 0.25f, 0.4f, 0f, 0.9f, 0f) to 0)
        list.add(floatArrayOf(0.75f, 0.6f, 0.15f, 0.2f, 0.5f, 1f, 1.0f, 0f) to 0)
        list.add(floatArrayOf(0.90f, 0.7f, 0.30f, 0.3f, 0.4f, 0f, 0.8f, 0f) to 0)
        if (role == HeroRole.ASSASSIN || role == HeroRole.WARRIOR) {
            list.add(floatArrayOf(0.80f, 0.7f, 0.55f, 0.22f, 0.5f, 0f, 0.9f, 0f) to 0)
            list.add(floatArrayOf(0.70f, 0.6f, 0.45f, 0.18f, 0.4f, 0f, 0.75f, 0f) to 0)
        }

        // Nhãn 1: KITE_POKE khi giao tranh tầm trung, máu ổn, địch chưa thấp máu
        list.add(floatArrayOf(0.75f, 0.7f, 0.70f, 0.42f, 0.4f, 0f, 0.6f, 0f) to 1)
        list.add(floatArrayOf(0.65f, 0.6f, 0.65f, 0.35f, 0.3f, 0f, 0.5f, 0.4f) to 1)
        if (role == HeroRole.MARKSMAN || role == HeroRole.MAGE) {
            list.add(floatArrayOf(0.80f, 0.75f, 0.55f, 0.30f, 0.4f, 0f, 0.7f, 0f) to 1)
            list.add(floatArrayOf(0.60f, 0.5f, 0.80f, 0.28f, 0.35f, 0f, 0.4f, 0f) to 1)
        }

        // Nhãn 2: PUSH_LANE khi địch ở xa hoặc địch ôm trụ đầy máu
        list.add(floatArrayOf(0.85f, 0.7f, 0.85f, 0.85f, 0.5f, 1f, 0.5f, 0f) to 2)
        list.add(floatArrayOf(0.75f, 0.6f, 0.70f, 0.75f, 0.6f, 1f, 0.4f, 0f) to 2)
        list.add(floatArrayOf(0.90f, 0.8f, 0.90f, 0.90f, 0.5f, 0f, 0.6f, 0f) to 2)

        // Nhãn 3: FARM_JUNGLE khi địch xa và đang chờ hồi chiêu
        list.add(floatArrayOf(0.70f, 0.5f, 0.95f, 0.92f, 0.4f, 1f, 0.2f, 0f) to 3)
        list.add(floatArrayOf(0.65f, 0.4f, 0.85f, 0.80f, 0.3f, 0f, 0.15f, 0f) to 3)

        return list
    }

    fun createPresetWeights(role: HeroRole): FloatArray {
        val rng = Random(role.ordinal * 1337 + 42)
        val raw = FloatArray(TOTAL_WEIGHTS) { (rng.nextFloat() - 0.5f) * 0.6f }
        return trainOnDevice(raw, role, retreatHpPercent = 26, epochs = 180, learningRate = 0.05f).updatedWeights
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
