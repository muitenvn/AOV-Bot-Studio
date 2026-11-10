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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
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
import com.example.service.AovBotAccessibilityService
import com.example.service.RealServiceState
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
    onSelectProfile: (Int) -> Unit,
    onChangeBrainMode: (BrainMode) -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onUpdateScanInterval: (Long) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onOpenTrainerSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var systemSettingEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabledInSystem(context))
    }

    // Tự động kiểm tra lại quyền ngay khi người dùng quay về từ màn hình Cài đặt Android
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemSettingEnabled = isAccessibilityServiceEnabledInSystem(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isReady = serviceState.isServiceConnected || systemSettingEnabled
    val activeBrainMode = runCatching { BrainMode.valueOf(activeProfile.brainMode) }.getOrDefault(BrainMode.HYBRID)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Banner Trung Tâm Điều Khiển Chạy Trên Game Thật
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_tactical_banner_1794320021323),
                    contentDescription = "Biểu ngữ điều khiển Bot trên game Liên Quân thật",
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
                                    ObsidianNavy.copy(alpha = 0.95f)
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
                                text = "CHẾ ĐỘ CHẠY TRỰC TIẾP TRÊN GAME THẬT",
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
                            color = if (isReady) EmeraldSuccess.copy(alpha = 0.18f) else CrimsonDanger.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(
                                1.dp,
                                if (isReady) EmeraldSuccess else CrimsonDanger,
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
                                        .background(if (isReady) EmeraldSuccess else CrimsonDanger)
                                )
                                Text(
                                    text = if (isReady) "ĐÃ BẬT HỖ TRỢ" else "CHƯA BẬT QUYỀN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReady) EmeraldSuccess else CrimsonDanger
                                )
                            }
                        }
                    }

                    // Chọn Hồ sơ Bot áp dụng cho Game Thật
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
                                modifier = Modifier.testTag("real_profile_chip_${prof.id}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Bảng Cấp Quyền Hỗ Trợ & Khởi Chạy Tự Động Quét Màn Hình
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (serviceState.isAutoPlaying) EmeraldSuccess else ElectricCyan.copy(alpha = 0.6f),
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
                        imageVector = Icons.Default.AccessibilityNew,
                        contentDescription = "Quyền Hỗ Trợ",
                        tint = if (isReady) EmeraldSuccess else AmberHighlight
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReady) {
                                "DỊCH VỤ QUÉT MÀN HÌNH & TỰ BẤM ĐÃ SẴN SÀNG"
                            } else {
                                "BƯỚC 1: BẬT QUYỀN HỖ TRỢ (ACCESSIBILITY) ĐỂ CHƠI GAME THẬT"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isReady) EmeraldSuccess else AmberHighlight
                        )
                        Text(
                            text = if (isReady) {
                                "Thanh điều khiển nổi [▶ CHẠY BOT] đã hiện trên màn hình. Hãy mở Liên Quân và bấm chạy!"
                            } else {
                                "Bấm nút bên dưới -> Chọn 'Ứng dụng đã cài đặt' -> Bật 'AOV Bot Tự Chơi Liên Quân'."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Nút Mở Cài Đặt Quyền Hỗ Trợ
                Button(
                    onClick = { openAccessibilitySettings(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReady) DeepSlateBlue else AmberHighlight,
                        contentColor = if (isReady) ElectricCyan else ObsidianNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_open_accessibility_settings")
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReady) {
                            "ĐÃ CẤP QUYỀN HỖ TRỢ (BẤM ĐỂ MỞ LẠI CÀI ĐẶT)"
                        } else {
                            "MỞ CÀI ĐẶT ANDROID ĐỂ CẤP QUYỀN HỖ TRỢ"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                // Nút BẬT / DỪNG Tự Chơi Trên Game Thật & Nút Mở Liên Quân Mobile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isReady) {
                                Toast.makeText(
                                    context,
                                    "Vui lòng bật Quyền Hỗ Trợ trong Cài đặt trước!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                openAccessibilitySettings(context)
                            } else {
                                onToggleAutoPlay(!serviceState.isAutoPlaying)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serviceState.isAutoPlaying) CrimsonDanger else EmeraldSuccess,
                            contentColor = if (serviceState.isAutoPlaying) Color.White else ObsidianNavy
                        ),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(50.dp)
                            .testTag("btn_toggle_real_autoplay")
                    ) {
                        Icon(
                            imageVector = if (serviceState.isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (serviceState.isAutoPlaying) "DỪNG QUÉT & BẤM" else "CHẠY BOT NGAY",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { launchRealAovGame(context) },
                        modifier = Modifier
                            .weight(0.95f)
                            .height(50.dp)
                            .testTag("btn_launch_aov_game")
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mở Liên Quân", fontWeight = FontWeight.Bold)
                    }
                }

                // Công tắc hiện Thanh Điều Khiển Nổi đè lên màn hình Game
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
                                    text = "Thanh điều khiển nổi trong Game (HUD Overlay)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Kéo thả tự do trên màn hình Liên Quân để Bật/Dừng Bot tức thì",
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

        // 3. Chọn Nhanh Chế Độ Bộ Não & Chu Kỳ Quét Màn Hình Thật
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "BỘ NÃO XỬ LÝ & TỐC ĐỘ QUÉT MÀN HÌNH THẬT",
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
                            BrainMode.RULE_FSM -> "1. Luật Sẵn (0% AI)"
                            BrainMode.MICRO_AI -> "2. Micro-AI Nội Bộ"
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

                // Thanh trượt chỉnh chu kỳ quét màn hình (ms)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chu kỳ chụp & quét màn hình liên tục:",
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
                        valueRange = 180f..900f,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldSuccess,
                            activeTrackColor = EmeraldSuccess
                        )
                    )
                }
            }
        }

        // 4. Bảng Giám Sát Dữ Liệu Quét Từ Màn Hình Thật (Real-time Pixel Scanner Telemetry)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
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
                            text = "THÔNG SỐ QUÉT MÀN HÌNH THẬT (TRỰC TIẾP)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "Quét: %.0fµs | Não: %.1fµs",
                            serviceState.lastScanTimeMicros,
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
                    StatColumn("KHUNG ĐÃ QUÉT", "${serviceState.framesScanned}", ElectricCyan)
                    StatColumn("CỬ CHỈ ĐÃ BẤM", "${serviceState.gesturesDispatched}", TacticalGold)
                    StatColumn(
                        "MỤC TIÊU ĐỊCH",
                        if (serviceState.enemyLockedOnScreen) "ĐÃ KHÓA ĐỎ" else "ĐANG DÒ TÌM",
                        if (serviceState.enemyLockedOnScreen) CrimsonDanger else MutedSteelGray
                    )
                }

                // Thanh máu quét được trên màn hình
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Máu tướng bạn (Quét thanh xanh tâm màn hình):", style = MaterialTheme.typography.labelSmall)
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
                        Text("Máu tướng địch (Quét thanh đỏ chiến trường):", style = MaterialTheme.typography.labelSmall)
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

                // Quyết định & Thao tác cảm ứng hiện tại
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
                            text = "Hành động: ${serviceState.currentActionTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberHighlight
                        )
                        Text(
                            text = "Thao tác gửi lên màn hình: ${serviceState.lastGestureSummary}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = serviceState.currentActionReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                // Nhật ký quét màn hình thật
                if (serviceState.liveLogs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Nhật ký quét & tự bấm gần nhất:",
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

                // Nút phụ chuyển sang Phòng Huấn Luyện AI (Game ảo để dạy AI trước khi vào trận thật)
                OutlinedButton(
                    onClick = onOpenTrainerSandbox,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_go_to_ai_trainer")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mở Phòng Huấn Luyện AI & Sân Tập Giả Lập (Chỉ dùng để dạy Bot)")
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
        "Đã sẵn sàng thanh nổi! Hãy mở game Liên Quân Mobile từ màn hình chính của bạn.",
        Toast.LENGTH_LONG
    ).show()
}
