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
import com.example.engine.AovHeroAndMetaDatabase
import com.example.engine.AovLane
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
    var selectedHeroId by remember(activeProfile.id) { mutableStateOf(activeProfile.selectedHeroId) }
    var assignedLane by remember(activeProfile.id) {
        mutableStateOf(runCatching { AovLane.valueOf(activeProfile.assignedLane) }.getOrDefault(AovLane.TOP_CAESAR))
    }
    var heroRole by remember(activeProfile.id) {
        mutableStateOf(runCatching { HeroRole.valueOf(activeProfile.heroRole) }.getOrDefault(HeroRole.WARRIOR))
    }
    var brainMode by remember(activeProfile.id) {
        mutableStateOf(runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID))
    }
    var retreatHp by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.retreatHpPercent.toFloat()) }
    var recallMana by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.recallManaPercent.toFloat()) }
    var diveEnemyHp by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.diveTowerEnemyHpPercent.toFloat()) }
    var kitingDist by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.kitingDistance.toFloat()) }
    var miaAlertCount by remember(activeProfile.id) { mutableFloatStateOf(activeProfile.minimapMiaAlertCount.toFloat()) }
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
    var autoSwapBlade by remember(activeProfile.id) { mutableStateOf(activeProfile.autoSwapResurrectionBlade) }
    var savedBannerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(activeProfile) {
        name = activeProfile.name
        selectedHeroId = activeProfile.selectedHeroId
        assignedLane = runCatching { AovLane.valueOf(activeProfile.assignedLane) }.getOrDefault(AovLane.TOP_CAESAR)
        heroRole = runCatching { HeroRole.valueOf(activeProfile.heroRole) }.getOrDefault(HeroRole.WARRIOR)
        brainMode = runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID)
        retreatHp = activeProfile.retreatHpPercent.toFloat()
        recallMana = activeProfile.recallManaPercent.toFloat()
        diveEnemyHp = activeProfile.diveTowerEnemyHpPercent.toFloat()
        kitingDist = activeProfile.kitingDistance.toFloat()
        miaAlertCount = activeProfile.minimapMiaAlertCount.toFloat()
        targetPriority = runCatching { TargetPriority.valueOf(activeProfile.targetPriority) }
            .getOrDefault(TargetPriority.LOWEST_HP_HERO)
        comboSequence = activeProfile.comboSequence
        autoDodge = activeProfile.autoDodgeSkillshot
        autoJungle = activeProfile.autoJungleWhenLaneClear
        autoBuy = activeProfile.autoBuyEquipment
        autoSwapBlade = activeProfile.autoSwapResurrectionBlade
    }

    val currentHeroSpec = AovHeroAndMetaDatabase.getHeroSpecByIdOrRole(selectedHeroId, heroRole.name)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Chọn Hồ sơ & Tạo Mới
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
                            contentDescription = "Cấu hình đầu não Bot",
                            tint = ElectricCyan
                        )
                        Column {
                            Text(
                                text = "CƠ CHẾ MÚA TƯỚNG • ĐI ĐƯỜNG • BIẾN VỀ",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Thiết lập cách chơi sâu cho từng tướng trong Liên Quân thật",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteelGray
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onCreateNewProfile(
                                "Bot Chiến Thuật #${profiles.size + 1}",
                                HeroRole.WARRIOR,
                                BrainMode.HYBRID
                            )
                        },
                        modifier = Modifier.testTag("btn_add_new_bot")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo Bot mới", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Thêm Bot", style = MaterialTheme.typography.labelSmall)
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

        // 2. Chọn Vị Tướng Cụ Thể & Cách "Múa" Đặc Trưng (Florentino nhặt hoa, Nakroth lướt ngược, Tulen tích ấn...)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. CHỌN TƯỚNG & CÁCH MÚA RIÊNG BIỆT (MICRO-COMBO)",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberHighlight
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hồ sơ chiến thuật") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bot_name")
                )

                Text(
                    text = "Chọn Tướng để nạp cơ chế vuốt chiêu / nhặt hoa / lướt ngược:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteelGray
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AovHeroAndMetaDatabase.ALL_HERO_SPECS.forEach { spec ->
                        val isSelected = spec.id == selectedHeroId
                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else ObsidianNavy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedHeroId = spec.id
                                    heroRole = spec.role
                                    assignedLane = spec.recommendedLane
                                    kitingDist = spec.role.baseRange
                                    comboSequence = spec.signatureComboSteps.joinToString(" -> ") { it.stepTitle }
                                    name = "${spec.heroName.substringBefore(" (")} - ${spec.recommendedLane.shortVi}"
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spec.heroName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ElectricCyan else Color.White
                                    )
                                    Text(
                                        text = "${spec.tierMeta} • Phép: ${spec.recommendedSpell}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberHighlight
                                    )
                                }
                                Text(
                                    text = spec.recommendedLane.shortVi,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                // Hiển thị chi tiết Cách Múa & Khi nào đánh của vị tướng đang chọn
                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CÁCH MÚA ${currentHeroSpec.heroName.uppercase()}:",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = currentHeroSpec.mechanicsSummaryVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "• Khi nào nên đánh: ${currentHeroSpec.whenToFightVi}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "• Khi nào nên biến về: ${currentHeroSpec.whenToRecallVi}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                        Text(
                            text = "• Chuỗi thao tác tay tự động:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                        currentHeroSpec.signatureComboSteps.forEach { step ->
                            Text(
                                text = "   ${step.stepTitle}: ${step.descriptionVi} (${step.delayAfterMs}ms)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Chọn Đường Đi (Lane) & Đọc Bản Đồ Nhỏ (Minimap MIA Chống Gank)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "2. ĐI ĐƯỜNG GÌ & ĐỌC BẢN ĐỒ NHỎ (MINIMAP VISION)",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )

                AovLane.entries.forEach { lane ->
                    val isSelected = lane == assignedLane
                    Surface(
                        color = if (isSelected) EmeraldSuccess.copy(alpha = 0.16f) else ObsidianNavy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { assignedLane = lane }
                            .border(
                                1.dp,
                                if (isSelected) EmeraldSuccess else Color(0xFF283A5E),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = lane.titleVi,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) EmeraldSuccess else Color.White
                            )
                            Text(
                                text = lane.macroStrategyVi,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteelGray
                            )
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số địch mất tích trên Minimap (MIA) thì lùi ôm trụ:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            ">= ${miaAlertCount.toInt()} Tướng",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberHighlight
                        )
                    }
                    Slider(
                        value = miaAlertCount,
                        onValueChange = { miaAlertCount = it },
                        valueRange = 1f..4f,
                        steps = 2,
                        colors = SliderDefaults.colors(thumbColor = AmberHighlight, activeTrackColor = AmberHighlight)
                    )
                }
            }
        }

        // 4. Khi Nào Biến Về, Khi Nào Băng Trụ & Khoảng Cách Thả Diều
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "3. KHI NÀO BIẾN VỀ • KHI NÀO ĐÁNH • LÊN ĐỒ",
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalGold
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu thấp tự lùi bụi/trụ & bấm Biến Về:", style = MaterialTheme.typography.bodyMedium)
                        Text("${retreatHp.toInt()}% HP", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                    }
                    Slider(
                        value = retreatHp,
                        onValueChange = { retreatHp = it },
                        valueRange = 12f..50f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldSuccess, activeTrackColor = EmeraldSuccess)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mana cạn tự bấm Biến Về (Pháp sư / Sát thủ):", style = MaterialTheme.typography.bodyMedium)
                        Text("${recallMana.toInt()}% Mana", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    }
                    Slider(
                        value = recallMana,
                        onValueChange = { recallMana = it },
                        valueRange = 5f..35f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu địch tối đa cho phép Băng Trụ kết liễu:", style = MaterialTheme.typography.bodyMedium)
                        Text("${diveEnemyHp.toInt()}% HP", style = MaterialTheme.typography.labelLarge, color = CrimsonDanger)
                    }
                    Slider(
                        value = diveEnemyHp,
                        onValueChange = { diveEnemyHp = it },
                        valueRange = 5f..35f,
                        colors = SliderDefaults.colors(thumbColor = CrimsonDanger, activeTrackColor = CrimsonDanger)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Khoảng cách giữ cự ly Thả Diều (Kiting):", style = MaterialTheme.typography.bodyMedium)
                        Text("${kitingDist.toInt()}px", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    }
                    Slider(
                        value = kitingDist,
                        onValueChange = { kitingDist = it },
                        valueRange = 120f..340f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                }

                Text(
                    text = "Thứ tự khóa mục tiêu ưu tiên:",
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

                TacticalToggleRow(
                    title = "Lách 90° né chiêu định hướng tự động",
                    subtitle = "Quét vòng cảnh báo đỏ và kéo Joystick vuông góc tức thì",
                    checked = autoDodge,
                    onCheckedChange = { autoDodge = it }
                )
                TacticalToggleRow(
                    title = "Tự động Mua Đồ Khắc Chế & Đổi Giáp Hộ Mệnh",
                    subtitle = "Tự mua Đao/Sách Truy Hồn, Giày Kiên Cường và đổi Liềm Đoạt Mệnh cuối trận",
                    checked = autoBuy,
                    onCheckedChange = {
                        autoBuy = it
                        autoSwapBlade = it
                    }
                )
                TacticalToggleRow(
                    title = "Tự động xoay ăn Rừng / Rồng / Caesar khi đẩy xong lính",
                    subtitle = "Tối ưu lượng vàng mỗi phút (GPM) và kiểm soát mục tiêu lớn",
                    checked = autoJungle,
                    onCheckedChange = { autoJungle = it }
                )

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
                                    selectedHeroId = selectedHeroId,
                                    assignedLane = assignedLane.name,
                                    heroRole = heroRole.name,
                                    brainMode = brainMode.name,
                                    retreatHpPercent = retreatHp.toInt(),
                                    recallManaPercent = recallMana.toInt(),
                                    diveTowerEnemyHpPercent = diveEnemyHp.toInt(),
                                    kitingDistance = kitingDist.toInt(),
                                    minimapMiaAlertCount = miaAlertCount.toInt(),
                                    targetPriority = targetPriority.name,
                                    comboSequence = comboSequence,
                                    autoDodgeSkillshot = autoDodge,
                                    autoJungleWhenLaneClear = autoJungle,
                                    autoBuyEquipment = autoBuy,
                                    autoSwapResurrectionBlade = autoSwapBlade
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
                        Text("LƯU CHIẾN THUẬT & ÁP DỤNG NGAY", fontWeight = FontWeight.Bold)
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
                            text = "Đã nạp cách múa ${currentHeroSpec.heroName} & chiến thuật đi đường vào bộ não Bot!",
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
