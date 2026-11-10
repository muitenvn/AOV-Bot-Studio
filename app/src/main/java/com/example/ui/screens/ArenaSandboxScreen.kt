package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.engine.ArenaSimulationEngine
import com.example.engine.ArenaSnapshot
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

@Composable
fun ArenaSandboxScreen(
    profiles: List<BotProfileEntity>,
    activeProfile: BotProfileEntity,
    arena: ArenaSnapshot,
    speedMultiplier: Int,
    onSelectProfile: (Int) -> Unit,
    onChangeBrainMode: (BrainMode) -> Unit,
    onTogglePause: () -> Unit,
    onCycleSpeed: () -> Unit,
    onResetMatch: () -> Unit,
    onTriggerScenario: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val activeBrainMode = runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hero Header Card với Banner Chiến Thuật & Chọn Hồ Sơ Bot
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_tactical_banner_1794320021323),
                    contentDescription = "Biểu ngữ trung tâm điều khiển chiến thuật MOBA",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianNavy.copy(alpha = 0.55f),
                                    ObsidianNavy.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AOV BOT STUDIO • XỬ LÝ TỨC THÌ",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = activeProfile.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(1.dp, EmeraldSuccess.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Độ trễ thực",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                                val realMicros = arena.lastDecision?.executionMicros ?: arena.avgDecisionMicros
                                Text(
                                    text = String.format(Locale.US, "%.1f µs (Thật)", realMicros),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    // Chọn nhanh Hồ sơ Bot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profiles.forEach { prof ->
                            val selected = prof.id == activeProfile.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectProfile(prof.id) },
                                label = {
                                    Text(
                                        text = prof.name.substringBefore(" -"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("profile_chip_${prof.id}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Chuyển nhanh 3 Chế độ Bộ Não Bot (Không cần mạng, độ trễ < 1ms)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHẾ ĐỘ BỘ NÃO ĐANG CHẠY",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberHighlight
                    )
                    Text(
                        text = activeBrainMode.subtitleVi,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteelGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrainMode.entries.forEach { mode ->
                        val isSelected = mode == activeBrainMode
                        val shortLabel = when (mode) {
                            BrainMode.RULE_FSM -> "1. Luật Sẵn (0% AI)"
                            BrainMode.MICRO_AI -> "2. Micro-AI Nội Bộ"
                            BrainMode.HYBRID -> "3. Kết Hợp Tình Huống"
                        }
                        Surface(
                            color = if (isSelected) ElectricCyan else ObsidianNavy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChangeBrainMode(mode) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricCyan else Color(0xFF2E3F63),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("brain_mode_${mode.name}")
                        ) {
                            Text(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ObsidianNavy else Color.White,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Sân Đấu Giả Lập Liên Quân Thời Gian Thực (Canvas 60 FPS)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF253759), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Thanh trạng thái KDA / Vàng / Điều khiển tốc độ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TelemetryBadge("K/D", "${arena.botKills}/${arena.botDeaths}", ElectricCyan)
                        TelemetryBadge("VÀNG", "${arena.botGold}G", TacticalGold)
                        TelemetryBadge(
                            "TRUNG BÌNH",
                            String.format(Locale.US, "%.1f µs", arena.avgDecisionMicros),
                            EmeraldSuccess
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_toggle_pause")
                        ) {
                            Icon(
                                imageVector = if (arena.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (arena.isPaused) "Tiếp tục trận đấu" else "Tạm dừng trận đấu",
                                tint = ElectricCyan
                            )
                        }
                        IconButton(
                            onClick = onCycleSpeed,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_cycle_speed")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Tăng tốc độ mô phỏng",
                                    tint = if (speedMultiplier > 1) TacticalGold else MutedSteelGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "x$speedMultiplier",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (speedMultiplier > 1) TacticalGold else MutedSteelGray
                                )
                            }
                        }
                        IconButton(
                            onClick = onResetMatch,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_reset_arena")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Đặt lại trận đấu",
                                tint = MutedSteelGray
                            )
                        }
                    }
                }

                // Bản đồ Sân Đấu
                MobaArenaCanvas(
                    arena = arena,
                    kitingDistance = activeProfile.kitingDistance.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF2B426B), RoundedCornerShape(14.dp))
                )

                // Quyết định hiện tại của Bộ não Bot
                val dec = arena.lastDecision
                if (dec != null) {
                    Surface(
                        color = ObsidianNavy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(dec.action.badgeColorHex))
                                    )
                                    Text(
                                        text = dec.action.titleVi,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(dec.action.badgeColorHex)
                                    )
                                }
                                Text(
                                    text = "Combo: ${dec.recommendedComboStep}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberHighlight
                                )
                            }
                            Text(
                                text = dec.reasonVi,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // HUD Chiêu thức 1 / 2 / Ulti / Hồi máu / Tốc biến & Trang bị đã lên
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkillHudBadge("Chiêu 1", arena.cdSkill1, ElectricCyan)
                    SkillHudBadge("Chiêu 2", arena.cdSkill2, AmberHighlight)
                    SkillHudBadge("ULTI", arena.cdUlt, TacticalGold)
                    SkillHudBadge("Cấp Cứu", arena.cdHeal, EmeraldSuccess)
                    SkillHudBadge("Tốc Biến", arena.cdFlicker, Color(0xFFB388FF))
                }

                Text(
                    text = "Trang bị tự mua: ${arena.botItemsOwned.joinToString(" • ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberHighlight
                )
            }
        }

        // 4. Bảng Thử Thách Tình Huống Thực Chiến (Bấm để xem Bot phản xạ)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Tình huống thử thách",
                        tint = TacticalGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "THẢ TÌNH HUỐNG ĐỂ THỬ TỐC ĐỘ PHẢN XẠ CỦA BOT",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScenarioButton(
                        title = "Địch Bắn Đạn",
                        subtitle = "Test Lách 90°",
                        accent = AmberHighlight,
                        onClick = { onTriggerScenario(1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scenario_dodge")
                    )
                    ScenarioButton(
                        title = "Địch 12% Máu",
                        subtitle = "Test Dồn Combo",
                        accent = ElectricCyan,
                        onClick = { onTriggerScenario(2) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scenario_dive")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScenarioButton(
                        title = "Bị Gank 18% HP",
                        subtitle = "Tốc Biến + Hồi Máu",
                        accent = CrimsonDanger,
                        onClick = { onTriggerScenario(3) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scenario_retreat")
                    )
                    ScenarioButton(
                        title = "Sạch Đợt Lính",
                        subtitle = "Xoay Ăn Bùa Đỏ",
                        accent = EmeraldSuccess,
                        onClick = { onTriggerScenario(4) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scenario_jungle")
                    )
                }
            }
        }

        // 5. Nhật ký Phản xạ Thời Gian Thực (Real nanoTime Telemetry)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NHẬT KÝ QUYẾT ĐỊNH (ĐO THẬT BẰNG NANOTIME)",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Tổng: ${arena.totalDecisionsCount} nhịp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteelGray
                    )
                }
                if (arena.decisionLogs.isEmpty()) {
                    Text(
                        text = "Đang khởi tạo dòng chảy dữ liệu chiến thuật...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSteelGray
                    )
                } else {
                    arena.decisionLogs.take(6).forEach { logLine ->
                        Text(
                            text = "• $logLine",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobaArenaCanvas(
    arena: ArenaSnapshot,
    kitingDistance: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.background(Color(0xFF0B1526))) {
        val sx = size.width / ArenaSimulationEngine.WORLD_W
        val sy = size.height / ArenaSimulationEngine.WORLD_H
        val scale = (sx + sy) / 2f

        // 1. Vẽ Sông (River) cắt chéo và Đường Giữa (Mid Lane)
        drawLine(
            color = Color(0xFF0E2A47),
            start = Offset(180f * sx, 60f * sy),
            end = Offset(820f * sx, 560f * sy),
            strokeWidth = 54f * scale
        )
        drawLine(
            color = Color(0xFF1A2D49),
            start = Offset(ArenaSimulationEngine.ALLY_FOUNTAIN_X * sx, ArenaSimulationEngine.ALLY_FOUNTAIN_Y * sy),
            end = Offset(ArenaSimulationEngine.ENEMY_FOUNTAIN_X * sx, ArenaSimulationEngine.ENEMY_FOUNTAIN_Y * sy),
            strokeWidth = 44f * scale
        )

        // 2. Vẽ Tế Đàn Đồng Minh & Địch
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.18f),
            radius = 52f * scale,
            center = Offset(ArenaSimulationEngine.ALLY_FOUNTAIN_X * sx, ArenaSimulationEngine.ALLY_FOUNTAIN_Y * sy)
        )
        drawCircle(
            color = CrimsonDanger.copy(alpha = 0.18f),
            radius = 52f * scale,
            center = Offset(ArenaSimulationEngine.ENEMY_FOUNTAIN_X * sx, ArenaSimulationEngine.ENEMY_FOUNTAIN_Y * sy)
        )

        // 3. Vẽ Vòng Tầm Bắn Trụ & Trụ hai bên
        val allyTowerPos = Offset(ArenaSimulationEngine.ALLY_TOWER_X * sx, ArenaSimulationEngine.ALLY_TOWER_Y * sy)
        val enemyTowerPos = Offset(ArenaSimulationEngine.ENEMY_TOWER_X * sx, ArenaSimulationEngine.ENEMY_TOWER_Y * sy)

        drawCircle(
            color = ElectricCyan.copy(alpha = 0.25f),
            radius = ArenaSimulationEngine.TOWER_RANGE * scale,
            center = allyTowerPos,
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        )
        drawCircle(
            color = CrimsonDanger.copy(alpha = 0.30f),
            radius = ArenaSimulationEngine.TOWER_RANGE * scale,
            center = enemyTowerPos,
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        )

        // Trụ đồng minh
        drawCircle(color = ElectricCyan, radius = 16f * scale, center = allyTowerPos)
        drawHpBar(allyTowerPos, 38f, 5f, arena.allyTowerHp / arena.allyTowerMaxHp, ElectricCyan)

        // Trụ địch
        drawCircle(color = TacticalGold, radius = 16f * scale, center = enemyTowerPos)
        drawHpBar(enemyTowerPos, 38f, 5f, arena.enemyTowerHp / arena.enemyTowerMaxHp, CrimsonDanger)

        // 4. Vẽ Quái Rừng (Bùa Đỏ)
        val junglePos = Offset(arena.jungleX * sx, arena.jungleY * sy)
        if (arena.jungleHp > 0f) {
            drawCircle(color = Color(0xFFFF6D00), radius = 13f * scale, center = junglePos)
            drawHpBar(junglePos, 30f, 4f, arena.jungleHp / arena.jungleMaxHp, Color(0xFFFF6D00))
        } else {
            drawCircle(
                color = MutedSteelGray.copy(alpha = 0.3f),
                radius = 11f * scale,
                center = junglePos,
                style = Stroke(width = 2f)
            )
        }

        // 5. Vẽ Lính hai phe
        for (m in arena.minions) {
            val mPos = Offset(m.x * sx, m.y * sy)
            val mColor = if (m.isAlly) Color(0xFF40C4FF) else Color(0xFFFF8A80)
            drawCircle(color = mColor, radius = 7.5f * scale, center = mPos)
            drawHpBar(mPos, 18f, 3f, m.hp / m.maxHp, mColor)
        }

        // 6. Vẽ Vector suy nghĩ của Bot & Vòng giữ khoảng cách Thả Diều
        val botPos = Offset(arena.botX * sx, arena.botY * sy)
        val targetPos = Offset(arena.botTargetX * sx, arena.botTargetY * sy)
        drawLine(
            color = EmeraldSuccess.copy(alpha = 0.55f),
            start = botPos,
            end = targetPos,
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        )
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.18f),
            radius = kitingDistance * scale,
            center = botPos,
            style = Stroke(width = 1.2f)
        )

        // 7. Vẽ Tướng Bot (Bạn) & Tướng Địch
        val enemyPos = Offset(arena.enemyX * sx, arena.enemyY * sy)
        drawCircle(color = CrimsonDanger, radius = 15f * scale, center = enemyPos)
        drawCircle(color = Color.White, radius = 15f * scale, center = enemyPos, style = Stroke(width = 2f))
        drawHpBar(enemyPos, 40f, 5.5f, arena.enemyHp / arena.enemyMaxHp, CrimsonDanger)

        drawCircle(color = ElectricCyan, radius = 16f * scale, center = botPos)
        drawCircle(color = EmeraldSuccess, radius = 16f * scale, center = botPos, style = Stroke(width = 2.5f))
        drawHpBar(botPos, 42f, 5.5f, arena.botHp / arena.botMaxHp, EmeraldSuccess)

        // 8. Vẽ Đạn & Chiêu định hướng
        for (p in arena.projectiles) {
            val pPos = Offset(p.x * sx, p.y * sy)
            drawCircle(
                color = Color(p.colorHex),
                radius = p.radius * scale,
                center = pPos
            )
            if (p.isSkillshot) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = (p.radius + 3f) * scale,
                    center = pPos,
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // 9. Vẽ chữ nổi chiến đấu (Floating Combat Text)
        for (ft in arena.floatingTexts.takeLast(6)) {
            val fx = (ft.x * sx).coerceIn(10f, size.width - 110f)
            val fy = (ft.y * sy).coerceIn(10f, size.height - 24f)
            drawText(
                textMeasurer = textMeasurer,
                text = ft.text,
                topLeft = Offset(fx, fy),
                style = TextStyle(
                    color = Color(ft.colorHex),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHpBar(
    center: Offset,
    barWidth: Float,
    barHeight: Float,
    ratio: Float,
    fillColor: Color
) {
    val left = center.x - barWidth / 2f
    val top = center.y - 24f
    drawRect(
        color = Color.Black.copy(alpha = 0.7f),
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight)
    )
    drawRect(
        color = fillColor,
        topLeft = Offset(left, top),
        size = Size(barWidth * ratio.coerceIn(0f, 1f), barHeight)
    )
}

@Composable
private fun TelemetryBadge(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedSteelGray)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun SkillHudBadge(name: String, cdSeconds: Float, color: Color) {
    val ready = cdSeconds <= 0.05f
    Surface(
        color = if (ready) color.copy(alpha = 0.18f) else ObsidianNavy,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(
            width = 1.dp,
            color = if (ready) color else Color(0xFF2B3B59),
            shape = RoundedCornerShape(10.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (ready) color else MutedSteelGray
            )
            Text(
                text = if (ready) "SẴN SÀNG" else String.format(Locale.US, "%.1fs", cdSeconds),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (ready) Color.White else CrimsonDanger
            )
        }
    }
}

@Composable
private fun ScenarioButton(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianNavy,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSteelGray
            )
        }
    }
}
