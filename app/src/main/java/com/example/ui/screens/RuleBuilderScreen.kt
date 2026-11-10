package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.data.TargetPriority
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedTacticalCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedSteelGray
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.TacticalGold

@Composable
fun RuleBuilderScreen(
    profiles: List<BotProfileEntity>,
    activeProfile: BotProfileEntity,
    onSelectProfile: (Int) -> Unit,
    onSaveProfile: (BotProfileEntity) -> Unit,
    onCreateNewProfile: (String, HeroRole, BrainMode) -> Unit,
    onDeleteProfile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var name by remember(activeProfile.id) { mutableStateOf(activeProfile.name) }
    var heroRole by remember(activeProfile.id) {
        mutableStateOf(runCatching { HeroRole.valueOf(activeProfile.heroRole) }.getOrDefault(HeroRole.MARKSMAN))
    }
    var brainMode by remember(activeProfile.id) {
        mutableStateOf(runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID))
    }
    var retreatHp by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.retreatHpPercent.toFloat()) }
    var diveEnemyHp by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.diveTowerEnemyHpPercent.toFloat()) }
    var kitingDist by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.kitingDistance.toFloat()) }
    var targetPriority by remember(activeProfile.id) {
        mutableStateOf(
            runCatching { TargetPriority.valueOf(activeProfile.targetPriority) }
                .getOrDefault(TargetPriority.LOWEST_HP_HERO)
        )
    }
    var comboSequence by remember(activeProfile.id) { mutableStateOf(activeProfile.comboSequence) }
    var autoDodge by remember(activeProfile.id) { mutableStateOf(activeProfile.autoDodgeSkillshot) }
    var autoJungle by remember(activeProfile.id) { mutableStateOf(activeProfile.autoJungleWhenLaneClear) }
    var autoBuy by remember(activeProfile.id) { mutableStateOf(activeProfile.autoBuyEquipment) }
    var savedBannerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        name = activeProfile.name
        heroRole = runCatching { HeroRole.valueOf(activeProfile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
        brainMode = runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID)
        retreatHp = activeProfile.retreatHpPercent.toFloat()
        diveEnemyHp = activeProfile.diveTowerEnemyHpPercent.toFloat()
        kitingDist = activeProfile.kitingDistance.toFloat()
        targetPriority = runCatching { TargetPriority.valueOf(activeProfile.targetPriority) }
            .getOrDefault(TargetPriority.LOWEST_HP_HERO)
        comboSequence = activeProfile.comboSequence
        autoDodge = activeProfile.autoDodgeSkillshot
        autoJungle = activeProfile.autoJungleWhenLaneClear
        autoBuy = activeProfile.autoBuyEquipment
    }

    val presetCombos = listOf(
        "S2 -> S3 -> S1 -> AA",
        "S1 -> S2 -> AA -> S3",
        "S3 -> S2 -> S1 -> AA",
        "S2 -> AA -> S1 -> AA"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Danh sách Hồ sơ Bot + Tạo mới
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Lập trình luật Bot",
                            tint = ElectricCyan
                        )
                        Column {
                            Text(
                                text = "TRÌNH LẬP TRÌNH LUẬT & CÂY HÀNH VI",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Thiết lập ngưỡng phản xạ 0ms không cần gọi đám mây",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteelGray
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onCreateNewProfile(
                                "Bot Tùy Chỉnh #${profiles.size + 1}",
                                HeroRole.WARRIOR,
                                BrainMode.HYBRID
                            )
                        },
                        modifier = Modifier.testTag("btn_add_new_bot")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo Bot mới", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Tạo Mới", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profiles.forEach { prof ->
                        FilterChip(
                            selected = prof.id == activeProfile.id,
                            onClick = { onSelectProfile(prof.id) },
                            label = { Text(prof.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }
            }
        }

        // Thông tin Tên & Chất tướng
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. ĐỊNH DANH & CHẤT TƯỚNG LIÊN QUÂN",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberHighlight
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên cấu hình Bot") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bot_name")
                )

                Text(
                    text = "Chọn Chất Tướng (Tự động tối ưu tầm đánh & tốc chạy):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteelGray
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeroRole.entries.forEach { roleItem ->
                        val isSelected = roleItem == heroRole
                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else ObsidianNavy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    heroRole = roleItem
                                    kitingDist = roleItem.baseRange
                                }
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else Color(0xFF283A5E),
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = roleItem.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ElectricCyan else Color.White
                                )
                                Text(
                                    text = "Tầm: ${roleItem.baseRange.toInt()}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteelGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ngưỡng Sinh Tồn, Băng Trụ & Khoảng Cách Thả Diều
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. CÔNG THỨC SINH TỒN & VỊ TRÍ (0MS DETERMINISTIC)",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )

                // Slider 1: Ngưỡng máu rút lui
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ngưỡng máu tự rút lui về trụ & Hồi máu:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${retreatHp.toInt()}% HP",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSuccess
                        )
                    }
                    Slider(
                        value = retreatHp,
                        onValueChange = { retreatHp = it },
                        valueRange = 10f..55f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess)
                    )
                }

                // Slider 2: Ngưỡng máu địch cho phép băng trụ
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu địch tối đa cho phép Băng Trụ kết liễu:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${diveEnemyHp.toInt()}% HP",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonDanger
                        )
                    }
                    Slider(
                        value = diveEnemyHp,
                        onValueChange = { diveEnemyHp = it },
                        valueRange = 5f..35f,
                        colors = SliderDefaults.colors(thumbColor = CrimsonDanger, activeTrackColor = CrimsonDanger)
                    )
                }

                // Slider 3: Khoảng cách thả diều
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Khoảng cách giữ vị trí Thả Diều (Kiting):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${kitingDist.toInt()} đơn vị",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }
                    Slider(
                        value = kitingDist,
                        onValueChange = { kitingDist = it },
                        valueRange = 120f..340f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }
            }
        }

        // Thứ tự ưu tiên Mục tiêu & Chuỗi Combo Kỹ năng
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. ƯU TIÊN MỤC TIÊU & CHUỖI COMBO CHIÊU THỨC",
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalGold
                )

                Text(
                    text = "Thứ tự khóa mục tiêu khi giao tranh:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteelGray
                )
                TargetPriority.entries.forEach { prio ->
                    val selected = prio == targetPriority
                    Surface(
                        color = if (selected) TacticalGold.copy(alpha = 0.18f) else ObsidianNavy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { targetPriority = prio }
                            .border(
                                1.dp,
                                if (selected) TacticalGold else Color(0xFF283A5E),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Text(
                            text = prio.labelVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) AmberHighlight else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chuỗi tung chiêu cực nhanh (S1=Chiêu 1, S2=Chiêu 2, S3=Ulti, AA=Đánh thường):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteelGray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetCombos.forEach { preset ->
                        FilterChip(
                            selected = comboSequence == preset,
                            onClick = { comboSequence = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // 3 Công tắc phản xạ tự động
                TacticalToggleRow(
                    title = "Phản xạ Lách 90° né chiêu định hướng",
                    subtitle = "Tính vector vuông góc với đường đạn bay tới trong < 0.01ms",
                    checked = autoDodge,
                    onCheckedChange = { autoDodge = it }
                )
                TacticalToggleRow(
                    title = "Tự động xoay sang Ăn Rừng / Bùa Đỏ",
                    subtitle = "Khi đã dọn hết đợt lính đường giữa và máu > ngưỡng rút lui",
                    checked = autoJungle,
                    onCheckedChange = { autoJungle = it }
                )
                TacticalToggleRow(
                    title = "Tự động mua Trang bị Đề xuất ngay khi đủ vàng",
                    subtitle = "Mua đồ tức thì không làm gián đoạn nhịp di chuyển",
                    checked = autoBuy,
                    onCheckedChange = { autoBuy = it }
                )

                // Nút Lưu & Xóa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSaveProfile(
                                activeProfile.copy(
                                    name = name,
                                    heroRole = heroRole.name,
                                    brainMode = brainMode.name,
                                    retreatHpPercent = retreatHp.toInt(),
                                    diveTowerEnemyHpPercent = diveEnemyHp.toInt(),
                                    kitingDistance = kitingDist.toInt(),
                                    targetPriority = targetPriority.name,
                                    comboSequence = comboSequence,
                                    autoDodgeSkillshot = autoDodge,
                                    autoJungleWhenLaneClear = autoJungle,
                                    autoBuyEquipment = autoBuy
                                )
                            )
                            savedBannerVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianNavy),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_bot_profile")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Lưu cấu hình")
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("LƯU CẤU HÌNH BOT", fontWeight = FontWeight.Bold)
                    }

                    if (profiles.size > 1) {
                        IconButton(
                            onClick = { onDeleteProfile(activeProfile.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, CrimsonDanger.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .testTag("btn_delete_bot_profile")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa cấu hình Bot",
                                tint = CrimsonDanger
                            )
                        }
                    }
                }

                if (savedBannerVisible) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Đã lưu",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Đã cập nhật luật chiến thuật vào bộ nhớ thiết bị! Sân đấu đã áp dụng ngay.",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = ObsidianNavy,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MutedSteelGray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianNavy,
                    checkedTrackColor = ElectricCyan
                )
            )
        }
    }
}
