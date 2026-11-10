package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BotProfileEntity
import com.example.engine.AovHeroAndMetaDatabase
import com.example.engine.AovLane
import com.example.engine.BrainDecisionResult
import com.example.engine.MicroBotBrain
import com.example.engine.TacticalInputState
import com.example.engine.TrainingReport
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedTacticalCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedSteelGray
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.TacticalGold
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MicroAiTrainerScreen(
    activeProfile: BotProfileEntity,
    testInput: TacticalInputState,
    lastTrainingReport: TrainingReport?,
    onUpdateTestInput: ((TacticalInputState) -> TacticalInputState) -> Unit,
    onUpdateEnemyCompTags: (String) -> Unit,
    onTrainEpochs: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val weights = remember(activeProfile.weightsCsv, lastTrainingReport) {
        lastTrainingReport?.updatedWeights ?: MicroBotBrain.parseWeightsOrDefault(activeProfile)
    }
    val sandboxDecision: BrainDecisionResult = remember(testInput, activeProfile, weights) {
        MicroBotBrain.decide(testInput, activeProfile, weights)
    }

    val currentLane = runCatching { AovLane.valueOf(activeProfile.assignedLane) }.getOrDefault(AovLane.TOP_CAESAR)
    val activeEnemyTags = remember(activeProfile.enemyCompTagsCsv) {
        activeProfile.enemyCompTagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    val draftRec = remember(currentLane, activeEnemyTags) {
        AovHeroAndMetaDatabase.analyzeDraftAndMeta(currentLane, activeEnemyTags)
    }
    val heroSpec = remember(activeProfile.selectedHeroId, activeProfile.heroRole) {
        AovHeroAndMetaDatabase.getHeroSpecByIdOrRole(activeProfile.selectedHeroId, activeProfile.heroRole)
    }

    val compTagOptions = listOf(
        "HEAL_TANK" to "Địch Hồi Máu Mạnh (Taara/Helen/Flo)",
        "HARD_CC" to "Nhiều Khống Chế Cứng (Aleister/Gildur)",
        "ASSASSIN_BURST" to "Sát Thủ Tàng Hình/Sốc Đam (Kaine/Ngộ Không)",
        "POKE_MAGE" to "Cấu Rỉa Tầm Xa (Elsu/Yue/Iggy)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. BỘ NÃO CẤM / CHỌN HỢP META & KHẮC CHẾ ĐỘI HÌNH ĐỊCH
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Cấm chọn hợp Meta",
                        tint = CrimsonDanger
                    )
                    Column {
                        Text(
                            text = "AI CẤM / CHỌN HỢP META & LÊN ĐỒ KHẮC CHẾ",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberHighlight
                        )
                        Text(
                            text = "Phân tích đội hình địch để chọn tướng, cấm tướng và đổi nhánh đồ tự động",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                // Danh sách Tướng Nên Cấm
                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NÊN CẤM CON GÌ TRONG RANK HIỆN TẠI (TOP BAN META):",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonDanger
                        )
                        draftRec.priorityBans.forEach { (banHero, reason) ->
                            Text(
                                text = "• $banHero: $reason",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Tướng khắc chế & bị khắc chế của tướng đang dùng
                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PHÂN TÍCH TƯỚNG ĐANG CHỌN: ${heroSpec.heroName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "• Khắc chế mạnh (Nên Pick khi địch có): ${heroSpec.strongAgainstHeroes.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "• Bị khắc chế bởi (Cẩn thận hoặc Cấm đi): ${heroSpec.counteredByHeroes.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonDanger
                        )
                        Text(
                            text = "• Trang bị chuẩn Meta: ${heroSpec.coreItems.joinToString(" → ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                    }
                }

                // Chọn đặc điểm đội hình địch để AI đổi nhánh mua đồ khắc chế
                Text(
                    text = "Đội hình địch trong trận có gì? (Bấm chọn để Bot tự đổi nhánh mua đồ khắc chế):",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    compTagOptions.forEach { (tagKey, labelVi) ->
                        val selected = tagKey in activeEnemyTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val nextSet = activeEnemyTags.toMutableSet()
                                if (selected) nextSet.remove(tagKey) else nextSet.add(tagKey)
                                onUpdateEnemyCompTags(nextSet.joinToString(","))
                            },
                            label = { Text(labelVi, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalGold.copy(alpha = 0.22f),
                                selectedLabelColor = AmberHighlight
                            )
                        )
                    }
                }

                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Text(
                                text = "CHIẾN THUẬT LÊN ĐỒ & ĐỐI ĐẦU TỰ ĐỘNG:",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldSuccess
                            )
                        }
                        draftRec.counterBuildAdvice.forEach { advice ->
                            Text(
                                text = "• $advice",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. HUẤN LUYỆN MẠNG NƠ-RON CHIẾN THUẬT TRÊN THIẾT BỊ (ON-DEVICE TACTICAL MLP)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Huấn luyện AI nội bộ",
                            tint = ElectricCyan
                        )
                        Column {
                            Text(
                                text = "HUẤN LUYỆN AI CHIẾN THUẬT TRÊN MÁY",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Dạy mạng nơ-ron khi nào đánh, khi nào biến về, khi nào né Gank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteelGray
                            )
                        }
                    }

                    Surface(
                        color = ObsidianNavy,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${activeProfile.trainedEpochs} Vòng",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                NeuralNetVisualizerCanvas(
                    inputs = testInput.toFloatArray8(),
                    weights = weights,
                    probabilities = sandboxDecision.actionProbabilities,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianNavy)
                        .border(1.dp, Color(0xFF263859), RoundedCornerShape(12.dp))
                )

                if (lastTrainingReport != null) {
                    Surface(
                        color = EmeraldSuccess.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Đã huấn luyện thêm +%d vòng trên CPU điện thoại trong %.2f ms • Sai số Loss: %.4f • Độ chuẩn chiến thuật: %.1f%%",
                                lastTrainingReport.epochsRun,
                                lastTrainingReport.trainingDurationMs,
                                lastTrainingReport.finalLoss,
                                lastTrainingReport.accuracyPercent
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onTrainEpochs(300) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianNavy),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_train_300")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Huấn Luyện +300 Vòng", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onTrainEpochs(1500) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_train_1500")
                    ) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Huấn Luyện Sâu +1500 Vòng", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 3. KIỂM TRA QUYẾT ĐỊNH CỦA ĐẦU NÃO BOT TRONG CÁC TÌNH HUỐNG THỰC CHIẾN
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KIỂM TRA ĐẦU NÃO PHÁN ĐOÁN TÌNH HUỐNG",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberHighlight
                    )
                    Text(
                        text = "Tỷ lệ thắng giao tranh: ${sandboxDecision.combatWinConfidencePct}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (sandboxDecision.combatWinConfidencePct >= 60) EmeraldSuccess else CrimsonDanger
                    )
                }

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(sandboxDecision.action.badgeColorHex), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PHÁN ĐOÁN: ${sandboxDecision.action.titleVi}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(sandboxDecision.action.badgeColorHex)
                        )
                        Text(
                            text = sandboxDecision.reasonVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Chuỗi Múa: ${sandboxDecision.recommendedComboStep}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Đồ Khắc Chế Ưu Tiên: ${sandboxDecision.recommendedCounterItem}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MicroBotBrain.OUTPUT_ACTIONS.forEachIndexed { idx, act ->
                        val prob = sandboxDecision.actionProbabilities.getOrElse(idx) { 0f }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = act.titleVi.substringBefore(" ("),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.width(150.dp)
                            )
                            LinearProgressIndicator(
                                progress = { prob.coerceIn(0f, 1f) },
                                color = Color(act.badgeColorHex),
                                trackColor = ObsidianNavy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "${(prob * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(act.badgeColorHex),
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }

                SandboxSlider(
                    label = "Máu của bạn hiện tại",
                    valueText = "${(testInput.botHpRatio * 100).toInt()}%",
                    value = testInput.botHpRatio,
                    color = EmeraldSuccess,
                    onValueChange = { v -> onUpdateTestInput { it.copy(botHpRatio = v) } }
                )
                SandboxSlider(
                    label = "Máu của Tướng Địch",
                    valueText = "${(testInput.enemyHpRatio * 100).toInt()}%",
                    value = testInput.enemyHpRatio,
                    color = CrimsonDanger,
                    onValueChange = { v -> onUpdateTestInput { it.copy(enemyHpRatio = v) } }
                )
                SandboxSlider(
                    label = "Số tướng địch mất tích trên Minimap (MIA)",
                    valueText = "${testInput.missingEnemiesMia} Tướng",
                    value = (testInput.missingEnemiesMia / 5f).coerceIn(0.05f, 1f),
                    color = AmberHighlight,
                    onValueChange = { v ->
                        onUpdateTestInput { it.copy(missingEnemiesMia = (v * 5f).toInt().coerceIn(0, 5)) }
                    }
                )
                SandboxSlider(
                    label = "Khoảng cách tới Địch trên màn hình",
                    valueText = "${(testInput.distToEnemyNorm * 600).toInt()}px",
                    value = testInput.distToEnemyNorm,
                    color = ElectricCyan,
                    onValueChange = { v -> onUpdateTestInput { it.copy(distToEnemyNorm = v) } }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Địch đang đứng ôm dưới Trụ:", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = testInput.enemyUnderTower > 0.5f,
                        onCheckedChange = { checked ->
                            onUpdateTestInput { it.copy(enemyUnderTower = if (checked) 1f else 0f) }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phát hiện đạn chiêu định hướng đang bay tới:", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = testInput.incomingProjectileThreat > 0.5f,
                        onCheckedChange = { checked ->
                            onUpdateTestInput { it.copy(incomingProjectileThreat = if (checked) 1f else 0f) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SandboxSlider(
    label: String,
    valueText: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = color)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.05f..1.0f,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

@Composable
private fun NeuralNetVisualizerCanvas(
    inputs: FloatArray,
    weights: FloatArray,
    probabilities: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val col1X = w * 0.14f
        val col2X = w * 0.50f
        val col3X = w * 0.86f

        val inputPositions = List(MicroBotBrain.INPUT_SIZE) { i ->
            Offset(col1X, h * (0.10f + 0.80f * (i.toFloat() / (MicroBotBrain.INPUT_SIZE - 1))))
        }
        val hiddenPositions = List(MicroBotBrain.HIDDEN_SIZE) { j ->
            Offset(col2X, h * (0.14f + 0.72f * (j.toFloat() / (MicroBotBrain.HIDDEN_SIZE - 1))))
        }
        val outputPositions = List(MicroBotBrain.OUTPUT_SIZE) { k ->
            Offset(col3X, h * (0.16f + 0.68f * (k.toFloat() / (MicroBotBrain.OUTPUT_SIZE - 1))))
        }

        var wIdx = 0
        for (j in hiddenPositions.indices) {
            for (i in inputPositions.indices) {
                val weightVal = weights.getOrElse(wIdx++) { 0.1f }
                val alpha = (abs(weightVal) * 0.45f).coerceIn(0.08f, 0.75f)
                val lineColor = if (weightVal >= 0f) ElectricCyan.copy(alpha = alpha) else CrimsonDanger.copy(alpha = alpha)
                drawLine(
                    color = lineColor,
                    start = inputPositions[i],
                    end = hiddenPositions[j],
                    strokeWidth = 1.6f
                )
            }
        }

        val w2Start = MicroBotBrain.INPUT_SIZE * MicroBotBrain.HIDDEN_SIZE + MicroBotBrain.HIDDEN_SIZE
        for (k in outputPositions.indices) {
            for (j in hiddenPositions.indices) {
                val weightVal = weights.getOrElse(w2Start + k * MicroBotBrain.HIDDEN_SIZE + j) { 0.1f }
                val alpha = (abs(weightVal) * 0.55f).coerceIn(0.10f, 0.85f)
                val lineColor = if (weightVal >= 0f) EmeraldSuccess.copy(alpha = alpha) else AmberHighlight.copy(alpha = alpha)
                drawLine(
                    color = lineColor,
                    start = hiddenPositions[j],
                    end = outputPositions[k],
                    strokeWidth = 2.0f
                )
            }
        }

        for (i in inputPositions.indices) {
            val v = inputs.getOrElse(i) { 0.5f }
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.35f + 0.65f * v),
                radius = 6.5f,
                center = inputPositions[i]
            )
        }

        for (j in hiddenPositions.indices) {
            drawCircle(
                color = AmberHighlight,
                radius = 7.5f,
                center = hiddenPositions[j]
            )
        }

        for (k in outputPositions.indices) {
            val p = probabilities.getOrElse(k) { 0.2f }
            drawCircle(
                color = EmeraldSuccess.copy(alpha = 0.3f + 0.7f * p),
                radius = 7f + p * 6f,
                center = outputPositions[k]
            )
        }
    }
}
