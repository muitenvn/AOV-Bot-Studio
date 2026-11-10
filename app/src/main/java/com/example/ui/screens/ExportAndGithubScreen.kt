package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BotProfileEntity
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.DeepSlateBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedTacticalCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedSteelGray
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.TacticalGold
import com.example.viewmodel.TouchButtonCoord

@Composable
fun ExportAndGithubScreen(
    activeProfile: BotProfileEntity,
    touchCoords: List<TouchButtonCoord>,
    onUpdateCoord: (String, Int, Int) -> Unit,
    exportConfigJson: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    var copiedConfig by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Sơ Đồ Tinh Chỉnh Tọa Độ Nút Bấm Màn Hình Thật (Áp dụng trực tiếp cho AccessibilityService)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Sơ đồ nút bấm",
                        tint = ElectricCyan
                    )
                    Column {
                        Text(
                            text = "TINH CHỈNH TỌA ĐỘ NÚT BẤM TRÊN GAME THẬT",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Dịch vụ Quyền Hỗ Trợ sẽ quét độ sáng & bấm vào đúng tọa độ X% / Y% này",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSteelGray
                        )
                    }
                }

                TouchLayoutPreviewCanvas(
                    coords = touchCoords,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianNavy)
                        .border(1.dp, Color(0xFF283C63), RoundedCornerShape(12.dp))
                )

                touchCoords.forEach { item ->
                    Surface(
                        color = ObsidianNavy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.labelVi,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ngang X: ${item.xPercent}% • Dọc Y: ${item.yPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberHighlight
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("X", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                IconButton(
                                    onClick = { onUpdateCoord(item.id, item.xPercent - 1, item.yPercent) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Giảm X", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { onUpdateCoord(item.id, item.xPercent + 1, item.yPercent) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tăng X", tint = ElectricCyan, modifier = Modifier.size(15.dp))
                                }

                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Y", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                IconButton(
                                    onClick = { onUpdateCoord(item.id, item.xPercent, item.yPercent - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Giảm Y", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                                IconButton(
                                    onClick = { onUpdateCoord(item.id, item.xPercent, item.yPercent + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tăng Y", tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Thẻ Tự Động Xây Dựng APK & Đăng Lên GitHub Releases
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Tự động phát hành GitHub Release",
                        tint = EmeraldSuccess
                    )
                    Column {
                        Text(
                            text = "TỰ ĐỘNG BUILD APK & ĐĂNG LÊN GITHUB RELEASES",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "File: .github/workflows/android-release.yml (Gradle 9.3.1 + API 36.1)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                    }
                }

                Text(
                    text = "Khi bạn đẩy mã nguồn lên GitHub, hệ thống sẽ tự động đóng gói file cài đặt AOV-Bot-Studio-v1.0.X.apk và đăng lên mục Releases (Phát hành) để bạn tải về điện thoại thật và cấp Quyền Hỗ Trợ chơi ngay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }

        // 3. Xuất & Sao Chép Kịch Bản Cấu Hình JSON + Trọng Số Bot
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSlateBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "XUẤT CẤU HÌNH BOT (${activeProfile.name})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalGold
                )

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportConfigJson,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportConfigJson))
                        copiedConfig = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_copy_export_json")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép cấu hình")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAO CHÉP CẤU HÌNH & TỌA ĐỘ BOT", fontWeight = FontWeight.Bold)
                }

                if (copiedConfig) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Đã sao chép",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Đã sao chép toàn bộ kịch bản Bot và tọa độ vào bộ nhớ tạm!",
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
private fun TouchLayoutPreviewCanvas(
    coords: List<TouchButtonCoord>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Khung bản đồ nhỏ góc trái trên (Minimap)
        drawRect(
            color = Color(0xFF162844),
            topLeft = Offset(w * 0.03f, h * 0.06f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.28f)
        )

        coords.forEach { btn ->
            val cx = w * (btn.xPercent / 100f)
            val cy = h * (btn.yPercent / 100f)
            val isJoystick = btn.id == "JOYSTICK"
            val isMainAttack = btn.id == "AA_HERO"
            val radius = when {
                isJoystick -> 26f
                isMainAttack -> 20f
                else -> 13f
            }
            val color = when {
                isJoystick -> ElectricCyan
                isMainAttack -> TacticalGold
                btn.id.startsWith("SKILL") -> EmeraldSuccess
                else -> AmberHighlight
            }

            drawCircle(
                color = color.copy(alpha = 0.22f),
                radius = radius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = btn.id.removePrefix("SKILL_").take(4),
                topLeft = Offset((cx - 12f).coerceAtLeast(2f), (cy - 6f).coerceAtLeast(2f)),
                style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
