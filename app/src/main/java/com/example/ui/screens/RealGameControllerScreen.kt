package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.service.ExecutionBackendMode
import com.example.service.RealServiceState
import com.example.service.ShizukuManagerEngine
import com.example.service.ShizukuStatusState
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
fun RealGameControllerScreen(
    profiles: List<BotProfileEntity>,
    activeProfile: BotProfileEntity,
    serviceState: RealServiceState,
    shizukuState: ShizukuStatusState,
    onSelectProfile: (Int) -> Unit,
    onChangeBrainMode: (BrainMode) -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onUpdateScanInterval: (Long) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onConnectShizuku: () -> Unit,
    onUnlockAccessibilityViaShizuku: () -> Unit,
    onToggleShizukuDirectLoop: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var systemSettingEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabledInSystem(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemSettingEnabled = isAccessibilityServiceEnabledInSystem(context)
                ShizukuManagerEngine.checkShizukuEnvironment(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isAccessibilityReady = serviceState.isServiceConnected || systemSettingEnabled
    val isAnyEngineReady = isAccessibilityReady || shizukuState.isShellPrivilegeGranted
    val activeBrainMode = runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Hồ Sơ Tướng & Trạng Thái Kết Nối Game Thật
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_tactical_banner_1794320021323),
                    contentDescription = "Trung tâm điều khiển Bot Liên Quân thật",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(122.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianNavy.copy(alpha = 0.55f),
                                    ObsidianNavy.copy(alpha = 0.96f)
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
                                text = "TƯỚNG: ${activeProfile.selectedHeroId} • ĐƯỜNG: ${activeProfile.assignedLane}",
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
                            color = if (isAnyEngineReady) EmeraldSuccess.copy(alpha = 0.18f) else AmberHighlight.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(
                                1.dp,
                                if (isAnyEngineReady) EmeraldSuccess else AmberHighlight,
                                RoundedCornerShape(50)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAnyEngineReady) EmeraldSuccess else AmberHighlight)
                                )
                                Text(
                                    text = when {
                                        shizukuState.isShizukuLoopRunning -> "SHIZUKU ĐANG CHẠY"
                                        serviceState.isAutoPlaying -> "TRỢ NĂNG ĐANG CHẠY"
                                        isAnyEngineReady -> "ĐÃ CÓ QUYỀN"
                                        else -> "CHỌN QUYỀN BÊN DƯỚI"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAnyEngineReady) EmeraldSuccess else AmberHighlight
                                )
                            }
                        }
                    }

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
                                        text = "${prof.selectedHeroId} (${prof.assignedLane.substringAfter("_")})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("real_profile_chip_${prof.id}")
                            )
                        }
                    }
                }
            }
        }

        // 2. BẢNG QUYỀN SHIZUKU (Dùng khi máy chặn Trợ năng hoặc muốn chạy bằng quyền ADB Shell mượt nhất)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (shizukuState.isShizukuLoopRunning) EmeraldSuccess else TacticalGold.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Quyền Shizuku",
                        tint = TacticalGold
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CÁCH 1: CHẠY BẰNG QUYỀN SHIZUKU (ADB SHELL MƯỢT NHẤT)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TacticalGold
                        )
                        Text(
                            text = "Nếu điện thoại chặn Trợ năng ('Cài đặt bị hạn chế'), dùng Shizuku để chạy trực tiếp hoặc ép mở khóa Trợ năng 1 chạm!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Trạng thái Shell: ${shizukuState.shellUidInfo}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (shizukuState.isShellPrivilegeGranted) EmeraldSuccess else AmberHighlight
                        )
                        Text(
                            text = shizukuState.lastStatusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConnectShizuku,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalGold,
                            contentColor = ObsidianNavy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_connect_shizuku")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhận Quyền Shizuku", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onUnlockAccessibilityViaShizuku,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_shizuku_unlock_accessibility")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ép Bật Trợ Năng", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { onToggleShizukuDirectLoop(!shizukuState.isShizukuLoopRunning) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (shizukuState.isShizukuLoopRunning) CrimsonDanger else EmeraldSuccess,
                        contentColor = if (shizukuState.isShizukuLoopRunning) Color.White else ObsidianNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_toggle_shizuku_direct")
                ) {
                    Icon(
                        imageVector = if (shizukuState.isShizukuLoopRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (shizukuState.isShizukuLoopRunning) {
                            "DỪNG LUỒNG SHIZUKU DIRECT SHELL"
                        } else {
                            "CHẠY BOT TRỰC TIẾP QUA SHIZUKU (KHÔNG CẦN TRỢ NĂNG)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. BẢNG QUYỀN HỖ TRỢ (ACCESSIBILITY SERVICE + THANH ĐIỀU KHIỂN NỔI TRONG GAME)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Quyền Hỗ Trợ",
                        tint = ElectricCyan
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CÁCH 2: CHẠY BẰNG QUYỀN HỖ TRỢ (CÓ THANH NỔI TRONG GAME)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Hiện nút [▶ CHẠY BOT / ⏸ DỪNG BOT] nổi đè lên màn hình Liên Quân để bật/tắt ngay trong trận",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { openAccessibilitySettings(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibilityReady) ElevatedTacticalCard else ElectricCyan,
                            contentColor = if (isAccessibilityReady) ElectricCyan else ObsidianNavy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_open_accessibility_settings")
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityReady) Icons.Default.CheckCircle else Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAccessibilityReady) "Đã Bật Trợ Năng" else "Mở Cài Đặt Trợ Năng",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { launchRealAovGame(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_launch_aov_game")
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mở Liên Quân", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Hiện Thanh Điều Khiển Nổi Trong Game",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hiển thị % Máu, Cảnh báo MIA Minimap & Hành động ngay góc màn hình",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteelGray
                                )
                            }
                        }
                        Switch(
                            checked = serviceState.isOverlayShown,
                            onCheckedChange = onToggleOverlay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianNavy,
                                checkedTrackColor = ElectricCyan
                            )
                        )
                    }
                }
            }
        }

        // 4. BỘ NÃO ĐANG CHẠY & TỐC ĐỘ QUÉT MÀN HÌNH THẬT
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CHẾ ĐỘ ĐẦU NÃO CỦA BOT & NHỊP QUÉT MÀN HÌNH",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberHighlight
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrainMode.entries.forEach { mode ->
                        val isSelected = mode == activeBrainMode
                        val label = when (mode) {
                            BrainMode.RULE_FSM -> "1. Luật Cứng (0% AI)"
                            BrainMode.MICRO_AI -> "2. AI Huấn Luyện"
                            BrainMode.HYBRID -> "3. Kết Hợp Hybrid"
                        }
                        Surface(
                            color = if (isSelected) ElectricCyan else ObsidianNavy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onChangeBrainMode(mode) }
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else Color(0xFF2E3F63),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("real_brain_mode_${mode.name}")
                        ) {
                            Text(
                                text = label,
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

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tốc độ quét màn hình & ra đòn:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${serviceState.scanIntervalMs} ms / nhịp",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSuccess
                        )
                    }
                    Slider(
                        value = serviceState.scanIntervalMs.toFloat(),
                        onValueChange = { onUpdateScanInterval(it.toLong()) },
                        valueRange = 160f..850f,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = EmeraldSuccess
                        )
                    )
                }
            }
        }

        // 5. THÔNG SỐ ĐỌC BẢN ĐỒ, GIAO TRANH, LÊN ĐỒ & MÚA TƯỚNG TRỰC TIẾP
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "PHÂN TÍCH TRẬN ĐẤU THẬT (BẢN ĐỒ • GIAO TRANH • ĐỒ)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "Não: %.1f µs",
                            serviceState.lastBrainTimeMicros
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn(
                        "TỶ LỆ THẮNG ĐÁNH",
                        "${serviceState.combatWinConfidencePct}%",
                        if (serviceState.combatWinConfidencePct >= 60) EmeraldSuccess else CrimsonDanger
                    )
                    StatColumn(
                        "ĐỊCH MẤT TÍCH (MIA)",
                        "${serviceState.minimapMiaCount} Tướng",
                        if (serviceState.minimapMiaCount >= activeProfile.minimapMiaAlertCount) AmberHighlight else ElectricCyan
                    )
                    StatColumn(
                        "ĐÃ QUÉT / BẤM",
                        "${serviceState.framesScanned}/${serviceState.gesturesDispatched}",
                        TacticalGold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu bản thân (Biến về khi <= ${activeProfile.retreatHpPercent}%):", style = MaterialTheme.typography.labelSmall)
                        Text("${serviceState.detectedBotHpPercent}%", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                    }
                    LinearProgressIndicator(
                        progress = { (serviceState.detectedBotHpPercent / 100f).coerceIn(0f, 1f) },
                        color = EmeraldSuccess,
                        trackColor = ObsidianNavy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu mục tiêu địch (Băng trụ khi <= ${activeProfile.diveTowerEnemyHpPercent}%):", style = MaterialTheme.typography.labelSmall)
                        Text("${serviceState.detectedEnemyHpPercent}%", style = MaterialTheme.typography.labelLarge, color = CrimsonDanger)
                    }
                    LinearProgressIndicator(
                        progress = { (serviceState.detectedEnemyHpPercent / 100f).coerceIn(0f, 1f) },
                        color = CrimsonDanger,
                        trackColor = ObsidianNavy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Quyết định: ${serviceState.currentActionTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberHighlight
                        )
                        Text(
                            text = "Thao tác Múa / Di chuyển: ${serviceState.lastGestureSummary}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Nhánh lên đồ khắc chế tự mua: ${serviceState.recommendedCounterItem}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = serviceState.currentActionReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                if (serviceState.liveLogs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Nhật ký đầu não xử lý trên Game Thật:",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                        serviceState.liveLogs.take(5).forEach { line ->
                            Text(
                                text = "• $line",
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
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedSteelGray)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private fun isAccessibilityServiceEnabledInSystem(context: Context): Boolean {
    return runCatching {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabledServices.contains(context.packageName, ignoreCase = true) &&
            enabledServices.contains("AovBotAccessibilityService", ignoreCase = true)
    }.getOrDefault(false)
}

private fun openAccessibilitySettings(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun launchRealAovGame(context: Context) {
    val pm = context.packageManager
    val packages = listOf(
        "com.garena.game.kgvn",
        "com.garena.game.kgtw",
        "com.ngame.allstar.eu"
    )
    for (pkg in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return
        }
    }
    Toast.makeText(
        context,
        "Hãy mở game Liên Quân Mobile trên máy để Bot bắt đầu quét màn hình!",
        Toast.LENGTH_LONG
    ).show()
}
