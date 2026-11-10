package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HeroRole(val displayName: String, val baseRange: Float, val baseSpeed: Float) {
    MARKSMAN("Xạ Thủ (Đường Rồng - Thả diều)", 265f, 3.3f),
    ASSASSIN("Sát Thủ (Đi Rừng - Bắt chủ lực)", 135f, 4.1f),
    MAGE("Pháp Sư (Đường Giữa - Cấu rỉa)", 250f, 3.2f),
    WARRIOR("Đấu Sĩ (Đường Caesar - Solo & Càn lướt)", 150f, 3.6f),
    SUPPORT("Trợ Thủ (Đi Roam - Soi Map & Bảo kê)", 165f, 3.4f)
}

enum class BrainMode(val titleVi: String, val subtitleVi: String) {
    RULE_FSM(
        "Lập trình sẵn (FSM / Cây hành vi)",
        "0% AI - Công thức toán học & luật ưu tiên, phản xạ < 0.01ms"
    ),
    MICRO_AI(
        "AI Chuyên Sâu Trên Máy (Tactical MLP)",
        "Mạng nơ-ron huấn luyện chiến thuật Liên Quân chạy trên CPU (~0.03ms)"
    ),
    HYBRID(
        "Kết hợp Tình huống (Adaptive Hybrid)",
        "Luật cứng cho Né đạn/Biến về/Minimap + AI chấm điểm Múa tướng & Giao tranh"
    )
}

enum class TargetPriority(val labelVi: String) {
    LOWEST_HP_HERO("Ưu tiên Tướng địch thấp máu nhất (Vét mạng)"),
    CARRY_MARKSMAN_MAGE("Ưu tiên khóa Xạ Thủ / Pháp Sư chủ lực địch"),
    NEAREST_ENEMY("Ưu tiên Mục tiêu gần nhất (Bắn an toàn)"),
    TOWER_AND_WAVE("Ưu tiên Dọn lính, Đẩy lẻ & Phá trụ"),
    JUNGLE_FARM("Ưu tiên Farm rừng, Rồng & Tà Thần Caesar")
}

@Entity(tableName = "bot_profiles")
data class BotProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val selectedHeroId: String = "FLORENTINO",
    val assignedLane: String = "TOP_CAESAR",
    val heroRole: String = HeroRole.WARRIOR.name,
    val brainMode: String = BrainMode.HYBRID.name,
    val retreatHpPercent: Int = 25,
    val recallManaPercent: Int = 15,
    val diveTowerEnemyHpPercent: Int = 15,
    val kitingDistance: Int = 240,
    val minimapMiaAlertCount: Int = 2,
    val targetPriority: String = TargetPriority.LOWEST_HP_HERO.name,
    val comboSequence: String = "S1 -> AA -> S2 -> Hoa -> S3",
    val enemyCompTagsCsv: String = "HEAL_TANK,HARD_CC",
    val autoDodgeSkillshot: Boolean = true,
    val autoJungleWhenLaneClear: Boolean = true,
    val autoBuyEquipment: Boolean = true,
    val autoSwapResurrectionBlade: Boolean = true,
    val weightsCsv: String = "",
    val trainedEpochs: Int = 0,
    val lastAccuracyPercent: Float = 94.5f,
    val updatedAt: Long = System.currentTimeMillis()
)
