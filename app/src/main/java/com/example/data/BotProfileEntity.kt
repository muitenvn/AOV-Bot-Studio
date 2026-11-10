package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HeroRole(val displayName: String, val baseRange: Float, val baseSpeed: Float) {
    MARKSMAN("Xạ Thủ (Thả diều)", 265f, 3.3f),
    ASSASSIN("Sát Thủ (Bắt chủ lực)", 135f, 4.1f),
    MAGE("Pháp Sư (Cấu rỉa)", 250f, 3.2f),
    WARRIOR("Đấu Sĩ (Càn lướt)", 150f, 3.6f),
    SUPPORT("Trợ Thủ (Bảo kê & Mở giao tranh)", 165f, 3.4f)
}

enum class BrainMode(val titleVi: String, val subtitleVi: String) {
    RULE_FSM(
        "Lập trình sẵn (FSM / Cây hành vi)",
        "0% AI - Chỉ dùng toán học & luật ưu tiên, phản xạ < 0.01ms"
    ),
    MICRO_AI(
        "AI Siêu Nhẹ Trên Máy (Micro-MLP 89 tham số)",
        "Mạng nơ-ron 2 tầng chạy trực tiếp trên CPU thiết bị (~0.03ms)"
    ),
    HYBRID(
        "Kết hợp Tình huống (Adaptive Hybrid)",
        "Luật cứng cho né đạn/trụ + Micro-AI chấm điểm giao tranh"
    )
}

enum class TargetPriority(val labelVi: String) {
    LOWEST_HP_HERO("Tướng địch thấp máu nhất"),
    NEAREST_ENEMY("Mục tiêu gần nhất (An toàn)"),
    TOWER_AND_WAVE("Ưu tiên Dọn lính & Đẩy trụ"),
    JUNGLE_FARM("Ưu tiên Farm rừng tích vàng")
}

@Entity(tableName = "bot_profiles")
data class BotProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val heroRole: String = HeroRole.MARKSMAN.name,
    val brainMode: String = BrainMode.HYBRID.name,
    val retreatHpPercent: Int = 25,
    val diveTowerEnemyHpPercent: Int = 15,
    val kitingDistance: Int = 240,
    val targetPriority: String = TargetPriority.LOWEST_HP_HERO.name,
    val comboSequence: String = "S2 -> S3 -> S1 -> AA",
    val autoDodgeSkillshot: Boolean = true,
    val autoJungleWhenLaneClear: Boolean = true,
    val autoBuyEquipment: Boolean = true,
    val weightsCsv: String = "",
    val trainedEpochs: Int = 0,
    val lastAccuracyPercent: Float = 86.5f,
    val updatedAt: Long = System.currentTimeMillis()
)
