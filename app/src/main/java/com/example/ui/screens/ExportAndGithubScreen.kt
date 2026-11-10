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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
        // 1. Thẻ Tự Động Xây Dựng APK & Đăng Lên GitHub Releases
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
                            text = "File cấu hình: .github/workflows/android-release.yml (Đã tích hợp sẵn)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberHighlight
                        )
                    }
                }

                Text(
                    text = "Hệ thống đã tạo sẵn quy trình tự động hóa GitHub Actions trong thư mục dự án. Khi bạn đồng bộ (Push) mã nguồn lên GitHub:\n" +
                        "1. Máy chủ GitHub sẽ tự động tải JDK 21 & Android SDK.\n" +
                        "2. Tự động đóng gói ra file cài đặt AOV-Bot-Studio-v1.0.X.apk.\n" +
                        "3. Tự động tạo bản phát hành mới trong mục Releases (Phát hành) ở góc phải trang GitHub của bạn để tải file APK về máy bất cứ lúc nào.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Surface(
                    color = ObsidianNavy,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mẹo: Trên GitHub, nếu muốn tạo lại bản APK mới thủ công, bạn chỉ cần vào tab 'Actions' -> chọn 'Build APK & Publish to GitHub Release' -> bấm nút 'Run workflow'.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 2. Sơ Đồ Tọa Độ Nút Bấm Màn Hình Liên Quân (Tỷ lệ %)
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
                            text = "SƠ ĐỒ TỌA ĐỘ NÚT ĐIỀU KHIỂN MÀN HÌNH NGANG",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Tương thích mọi độ phân giải màn hình theo tỷ lệ X% / Y%",
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.labelVi,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "X: ${item.xPercent}% • Y: ${item.yPercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = AmberHighlight
                            )
                        }
                    }
                }
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

        // 4. Lưu ý An toàn Tài khoản & Chính sách Hệ thống
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedTacticalCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "An toàn tài khoản",
                    tint = AmberHighlight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Thiết kế An toàn Chống Khóa Acc: Ứng dụng chạy độc lập dưới dạng Phòng huấn luyện chiến thuật & Giả lập Micro-AI tốc độ cao, hoàn toàn không chèn mã độc hay dịch vụ Trợ năng (Accessibility) vào bộ nhớ game Liên Quân, giúp bảo vệ 100% tài khoản game của bạn trước hệ thống quét gian lận.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSteelGray
                )
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
