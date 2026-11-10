package com.example.engine

import com.example.data.HeroRole

enum class AovLane(
    val titleVi: String,
    val shortVi: String,
    val minimapTargetXRatio: Float,
    val minimapTargetYRatio: Float,
    val macroStrategyVi: String
) {
    TOP_CAESAR(
        "Đường Tà Thần Caesar (Top Solo)",
        "Đường Caesar",
        0.22f,
        0.22f,
        "Ưu tiên dọn lính nhanh ở mép bụi sông, giữ máu solo 1v1. Khi rừng địch lộ ở đường Rồng thì ép trụ hoặc ăn Dơi Quỷ / Tà Thần."
    ),
    JUNGLE_CORE(
        "Đi Rừng (Jungle Gank & Mục Tiêu)",
        "Đi Rừng",
        0.38f,
        0.42f,
        "Khởi đầu Bùa cùng cánh Tà Thần lúc 0:30 -> Dọn 4 bãi lên Cấp 4 lúc 1:12 -> Gank đường Rồng -> Kiểm soát Rồng Ánh Sáng & Caesar."
    ),
    MID_ROYAL(
        "Đường Giữa (Mid Đảo Gank 2 Cánh)",
        "Đường Giữa",
        0.50f,
        0.50f,
        "Dùng chiêu diện rộng dọn 3 lính đầu trong 12 giây -> Di chuyển theo đường sông sau lưng Rừng nhà để tạo thế đánh 3v2 ở hai cánh."
    ),
    BOT_DRAGON(
        "Đường Rồng (Xạ Thủ Chủ Lực ADC)",
        "Đường Rồng",
        0.78f,
        0.78f,
        "Giữ khoảng cách tối đa sau lính và Trợ Thủ, tuyệt đối không bước qua vạch sông khi trên Minimap mất tích >= 2 địch (MIA >= 2)."
    ),
    SUPPORT_ROAM(
        "Trợ Thủ (Soi Map & Bảo Kê Chủ Lực)",
        "Trợ Thủ Roam",
        0.62f,
        0.65f,
        "Phút 0:00 giúp Mid dọn lính -> Kiểm tra Bùa rừng địch -> Từ phút 1:10 theo sát bảo kê Xạ Thủ, đứng chiếm bụi cỏ mở tầm nhìn."
    )
}

enum class GamePhase(val titleVi: String, val strategyRuleVi: String) {
    EARLY_LINING(
        "Đầu Trận (0:00 - 4:00): Đi Đường & Giữ Nhịp Farm",
        "Không băng trụ trước cấp 4. Ưu tiên ăn trọn vàng từ lính, giữ máu > 40%, nhìn Minimap tránh rừng địch gank lúc 1:15."
    ),
    MID_ROTATION(
        "Giữa Trận (4:00 - 12:00): Đảo Đường, Ép Trụ & Ăn Mục Tiêu",
        "Đẩy xong lính là xoay vào rừng hoặc hội quân ăn Rồng/Caesar. Chỉ mở giao tranh khi quân số đồng minh >= quân số địch trên Minimap."
    ),
    LATE_TEAMFIGHT(
        "Cuối Trận (12:00+): Giao Tranh Tổng & Đổi Đồ Nhanh",
        "Không đi lẻ khi địch MIA >= 3. Xạ thủ/Pháp sư đứng sau Đỡ đòn, Sát thủ rình bụi bắt chủ lực địch, tự động đổi Giáp Hộ Mệnh -> Liềm Đoạt Mệnh."
    )
}

data class ComboGestureStep(
    val stepTitle: String,
    val buttonId: String,          // "SKILL_1", "SKILL_2", "SKILL_3", "AA_HERO", "JOYSTICK", "SPELL_HEAL"
    val gestureType: String,       // "TAP", "SWIPE_TO_ENEMY", "SWIPE_REVERSE_ENEMY", "JOYSTICK_FLOWER_120", "JOYSTICK_KITE"
    val delayAfterMs: Long,
    val descriptionVi: String
)

data class HeroMechanicsSpec(
    val id: String,
    val heroName: String,
    val role: HeroRole,
    val recommendedLane: AovLane,
    val tierMeta: String,
    val recommendedSpell: String,
    val mechanicsSummaryVi: String,
    val whenToFightVi: String,
    val whenToRecallVi: String,
    val coreItems: List<String>,
    val situationalCounterItems: Map<String, String>,
    val counteredByHeroes: List<String>,
    val strongAgainstHeroes: List<String>,
    val signatureComboSteps: List<ComboGestureStep>
)

data class DraftRecommendation(
    val priorityBans: List<Pair<String, String>>,
    val bestPicksForLane: List<HeroMechanicsSpec>,
    val counterBuildAdvice: List<String>
)

object AovHeroAndMetaDatabase {

    val ALL_HERO_SPECS: List<HeroMechanicsSpec> = listOf(
        HeroMechanicsSpec(
            id = "FLORENTINO",
            heroName = "Florentino (Kiếm Sĩ Hào Hoa)",
            role = HeroRole.WARRIOR,
            recommendedLane = AovLane.TOP_CAESAR,
            tierMeta = "S+ (Đỉnh Meta Kỹ Năng)",
            recommendedSpell = "Tốc Biến / Bộc Phá",
            mechanicsSummaryVi = "Múa Nhặt Hoa Chuẩn: Vuốt Chiêu 1 (Hào Hoa) trúng địch -> Đánh thường đột kích -> Chém Chiêu 2 nhịp 1 -> Kéo Joystick lệch góc 120° nhặt Hoa 1 -> Đánh thường -> Chiêu 2 nhịp 2 -> Nhặt Hoa 2 -> Khóa Chiêu 3 (Tài Hoa) miễn khống chế -> Nhặt Hoa 3.",
            whenToFightVi = "Đánh ngay khi Chiêu 1 vừa găm trúng mục tiêu tạo ra 3 bông hoa. Nếu trượt Chiêu 1 và không có Chiêu 3, lập tức kéo Joystick lùi đợi 4 giây hồi chiêu.",
            whenToRecallVi = "Biến về khi Máu < 22% và không có hoa/lính để chém hồi máu, hoặc sau khi solo hạ gục Top địch và đẩy lính vào trụ.",
            coreItems = listOf("Giày Kiên Cường", "Thương Longinus", "Áo Choàng Băng Giá", "Huân Chương Troy", "Giáp Hộ Mệnh", "Nanh Fenrir"),
            situationalCounterItems = mapOf(
                "Địch nhiều hồi máu (Taara, Lữ Bố, Helen)" to "Đao Truy Hồn (lên ngay món 2)",
                "Địch sát thương phép sốc (Yue, Tulen)" to "Huân Chương Troy + Giày Kiên Cường",
                "Địch sát thủ vật lý (Nakroth, Ngộ Không)" to "Khiên Thất Truyền"
            ),
            counteredByHeroes = listOf("Aleister", "Arum", "Omen", "Chaugnar"),
            strongAgainstHeroes = listOf("Yena", "Zuka", "Arthur", "Skud", "Toro"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Phóng Hoa (S1)", "SKILL_1", "SWIPE_TO_ENEMY", 140L, "Vuốt Chiêu 1 định hướng vào vị trí địch"),
                ComboGestureStep("2. Đột Kích (AA)", "AA_HERO", "TAP", 110L, "Bấm Đánh thường lao vào mục tiêu"),
                ComboGestureStep("3. Thưởng Hoa 1 (S2)", "SKILL_2", "TAP", 95L, "Bấm Chiêu 2 nhịp 1 gây làm chậm"),
                ComboGestureStep("4. Bước Nhặt Hoa 1", "JOYSTICK", "JOYSTICK_FLOWER_120", 150L, "Kéo Joystick góc 120° nhặt bông hoa đầu tiên"),
                ComboGestureStep("5. Đột Kích + S2 Nhịp 2", "SKILL_2", "TAP", 120L, "Đánh thường + Chém Chiêu 2 nhịp 2 hất tung"),
                ComboGestureStep("6. Khóa Ultimate (S3)", "SKILL_3", "SWIPE_TO_ENEMY", 140L, "Vuốt Chiêu 3 lướt tới mục tiêu tạo thêm 3 hoa mới"),
                ComboGestureStep("7. Bước Nhặt Hoa 2 & S2 Nhịp 3", "JOYSTICK", "JOYSTICK_FLOWER_120", 150L, "Tiếp tục nhặt hoa hoàn tất chuỗi liên hoàn")
            )
        ),

        HeroMechanicsSpec(
            id = "NAKROTH",
            heroName = "Nakroth (Lưỡi Đao Phán Xét)",
            role = HeroRole.ASSASSIN,
            recommendedLane = AovLane.JUNGLE_CORE,
            tierMeta = "S+ (Ông Hoàng Đẩy Lẻ & Bắt Chủ Lực)",
            recommendedSpell = "Trừng Trị",
            mechanicsSummaryVi = "Lướt Ngược Chiêu 2: Chiêu 2 của Nakroth lướt NGƯỢC hướng vuốt! Bot tự động vuốt Chiêu 2 ngược 180° với vị trí địch để lướt tới áp sát -> Đánh thường quét ngang -> Chiêu 1 nhịp 1 hất tung -> Chiêu 3 (Gươm Hành Quyết miễn khống) -> Chiêu 1 nhịp 2 nhảy rút lui qua tường.",
            whenToFightVi = "Chỉ lao vào khi trên Minimap thấy Xạ thủ/Pháp sư địch tách khỏi Trợ thủ, hoặc khi địch còn < 55% máu. Khi địch đi cụm 5 người, chuyển sang chế độ Đẩy Lẻ 2 cánh (Split-push).",
            whenToRecallVi = "Biến về khi Máu < 25% hoặc Mana < 15% (Nakroth rất tốn mana khi lướt liên tục) mà không có Bùa Xanh.",
            coreItems = listOf("Kiếm Truy Hồn", "Giày Kiên Cường", "Thương Xuyên Phá", "Phức Hợp Kiếm", "Thánh Kiếm", "Giáp Hộ Mệnh"),
            situationalCounterItems = mapOf(
                "Địch nhiều Đỡ đòn giáp dày" to "Kiếm Muramasa (xuyên 40% giáp)",
                "Pháp sư địch cấu rỉa mạnh" to "Huân Chương Troy",
                "Cuối trận tiền dư" to "Đổi Giáp Hộ Mệnh -> Liềm Đoạt Mệnh"
            ),
            counteredByHeroes = listOf("Aleister", "Arum", "Kriknak", "Rourke"),
            strongAgainstHeroes = listOf("Elsu", "Yue", "Tel'Annas", "Krixi", "Yorn"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Lướt Ngược S2 Áp Sát", "SKILL_2", "SWIPE_REVERSE_ENEMY", 130L, "Vuốt Chiêu 2 ngược 180° để lướt thẳng vào địch"),
                ComboGestureStep("2. Đánh Thường Cường Hóa", "AA_HERO", "TAP", 100L, "Chém ngang tầm xa sau khi lướt S2"),
                ComboGestureStep("3. Bồi Thẩm Đoàn Nhịp 1 (S1)", "SKILL_1", "SWIPE_TO_ENEMY", 120L, "Hất tung mục tiêu tại chỗ"),
                ComboGestureStep("4. Gươm Hành Quyết (S3)", "SKILL_3", "TAP", 420L, "Chém liên hoàn miễn khống chế + hất tung cuối"),
                ComboGestureStep("5. S1 Nhịp 2 Rút Lui", "SKILL_1", "SWIPE_REVERSE_ENEMY", 120L, "Nhảy S1 lần 2 lùi ra khỏi tầm nguy hiểm")
            )
        ),

        HeroMechanicsSpec(
            id = "HAYATE",
            heroName = "Hayate (Bóng Ma Phi Tiêu)",
            role = HeroRole.MARKSMAN,
            recommendedLane = AovLane.BOT_DRAGON,
            tierMeta = "S+ (Xạ Thủ Sát Thương Chuẩn Diệt Tank)",
            recommendedSpell = "Thanh Tẩy / Tốc Biến",
            mechanicsSummaryVi = "Thả Diều Song Song: Vừa kéo Joystick ngang với hướng di chuyển của địch vừa vuốt Chiêu 1 (Chuỗi Phi Tiêu) để găm đủ 6 tầng nội tại Sát Thương Chuẩn -> Khi địch mất chiêu khống chế, bấm Chiêu 3 (Bão Phi Tiêu) + Thanh Tẩy bay vào giữa đội hình địch.",
            whenToFightVi = "Đánh mạnh từ khi có Giày Du Mục + Song Đao Bão Táp (đạt ngưỡng 150% tốc đánh). Chỉ bấm Ultimate S3 khi Nội năng >= 70% và đã tích sẵn nội tại lên mục tiêu trước mặt.",
            whenToRecallVi = "Biến về khi Máu < 28% (vì Hayate không có hút máu mạnh đầu trận) sau khi đã dùng S1 dọn sạch đợt lính dưới trụ.",
            coreItems = listOf("Giày Du Mục", "Song Đao Bão Táp", "Vuốt Hung Tàn", "Áo Choàng Băng Giá", "Gươm Uriel", "Giáp Hộ Mệnh"),
            situationalCounterItems = mapOf(
                "Địch nhiều khống chế cứng (Gildur, Veera)" to "Mang phép Thanh Tẩy + Gươm Uriel",
                "Sát thủ vật lý sốc đam (Ngộ Không, Zuka)" to "Khiên Thất Truyền"
            ),
            counteredByHeroes = listOf("Veera", "Kaine", "Paine", "Valhein"),
            strongAgainstHeroes = listOf("Thane", "Toro", "Baldum", "Maloch", "Arthur"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Kéo Thả Diều + S1 Phi Tiêu", "SKILL_1", "SWIPE_TO_ENEMY", 180L, "Vuốt Chiêu 1 theo hướng địch tích nội tại chuẩn"),
                ComboGestureStep("2. Đánh Thường Tích Ấn", "AA_HERO", "TAP", 110L, "Bắn thường kích hoạt sát thương chuẩn"),
                ComboGestureStep("3. Lướt Bóng Mờ (S2)", "SKILL_2", "JOYSTICK_KITE", 120L, "Lướt giữ khoảng cách an toàn"),
                ComboGestureStep("4. Bão Phi Tiêu (S3)", "SKILL_3", "SWIPE_TO_ENEMY", 150L, "Xoay Ultimate gây sát thương chuẩn diện rộng"),
                ComboGestureStep("5. Bấm Thanh Tẩy / Cấp Cứu", "SPELL_HEAL", "TAP", 90L, "Chống bị ngắt chiêu cuối khi đang xoay")
            )
        ),

        HeroMechanicsSpec(
            id = "TULEN",
            heroName = "Tulen (Hoàng Tử Lôi Quang)",
            role = HeroRole.MAGE,
            recommendedLane = AovLane.MID_ROYAL,
            tierMeta = "S (Pháp Sư Cơ Động & Vét Mạng)",
            recommendedSpell = "Tốc Biến",
            mechanicsSummaryVi = "Tích 5 Ấn Lôi Điện: Dùng Chiêu 2 (Lôi Động) lướt sát mục tiêu (1 ấn) -> Bắn Chiêu 1 (Lôi Quang) ở cự ly gần trúng cả 3 tia (3 ấn) -> Chiêu 2 nhịp 2 lướt ra (đủ 5 ấn kích hoạt 5 tia sét tự động) -> Khóa Chiêu 3 (Lôi Điểu) kết liễu địch thấp máu để hồi lại Lôi Điểu liên tục.",
            whenToFightVi = "Rất mạnh ngay từ Cấp 2! Luôn chủ động ép đường giữa và đi gank sớm. Khi địch còn < 32% máu, khóa ngay Chiêu 3 (Lôi Điểu).",
            whenToRecallVi = "Biến về khi Máu < 26% hoặc Mana < 18% sau khi dọn xong đợt lính đường giữa.",
            coreItems = listOf("Giày Kiên Cường", "Mặt Nạ Berith", "Trượng Bùng Nổ", "Vương Miện Hecate", "Trượng Hỗn Mang", "Quả Cầu Băng Sương"),
            situationalCounterItems = mapOf(
                "Địch có Helen / Taara / Florentino" to "Sách Truy Hồn (giảm 40% hồi máu)",
                "Địch nhiều kháng phép" to "Trượng Hỗn Mang sớm ở món 4"
            ),
            counteredByHeroes = listOf("Yue", "Iggy", "Aleister", "Chaugnar"),
            strongAgainstHeroes = listOf("Krixi", "Natalya", "Raz", "Nakroth"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Lôi Động Áp Sát (S2)", "SKILL_2", "SWIPE_TO_ENEMY", 120L, "Lướt S2 vào tầm gần làm chậm và lấy 1 ấn"),
                ComboGestureStep("2. Lôi Quang 3 Tia (S1)", "SKILL_1", "SWIPE_TO_ENEMY", 140L, "Bắn S1 cự ly gần trúng 3 tia lấy thêm 3 ấn"),
                ComboGestureStep("3. Lôi Động Lùi Giữ Vị Trí (S2)", "SKILL_2", "SWIPE_REVERSE_ENEMY", 120L, "Lướt S2 ra ngoài kích hoạt đủ 5 ấn Lôi Điện"),
                ComboGestureStep("4. Khóa Lôi Điểu Kết Liễu (S3)", "SKILL_3", "TAP", 150L, "Khóa mục tiêu thấp máu nhất để hồi 80% chiêu cuối")
            )
        ),

        HeroMechanicsSpec(
            id = "LILIANA",
            heroName = "Liliana (Cửu Vĩ Hồ Biến Hình)",
            role = HeroRole.MAGE,
            recommendedLane = AovLane.MID_ROYAL,
            tierMeta = "S (Pháp Sư Đa Dụng Toàn Diện)",
            recommendedSpell = "Tốc Biến",
            mechanicsSummaryVi = "Luân Chuyển Người - Cáo: Ở dạng Người đứng xa cấu rỉa S2 (Hồ Quang Đạn làm choáng) + S1 (Hồ Quang Lực). Khi địch còn dưới 45% máu, bấm S3 biến dạng Cáo lướt xuyên -> Đánh thường cào -> S2 Cáo lướt -> Bấm S2 lần 2 thả cầu Vũ Điệu Yêu Hồ -> S3 quay về dạng Người.",
            whenToFightVi = "Chỉ biến Cáo lao vào khi địch đã dưới 45% máu hoặc khi cần dùng khung hình miễn thương của S3 để né sát thương lớn.",
            whenToRecallVi = "Biến về khi Máu < 25% hoặc Mana < 15%.",
            coreItems = listOf("Giày Phù Thủy", "Trượng Bùng Nổ", "Vương Miện Hecate", "Sách Truy Hồn", "Trượng Hỗn Mang", "Quả Cầu Băng Sương"),
            situationalCounterItems = mapOf(
                "Địch nhiều sát thủ áp sát" to "Quả Cầu Băng Sương + Giày Kiên Cường",
                "Địch nhiều Đỡ đòn" to "Mặt Nạ Berith + Gươm Tận Thế (cào dạng Cáo)"
            ),
            counteredByHeroes = listOf("Kaine", "Paine", "Aleister"),
            strongAgainstHeroes = listOf("Krixi", "Veera", "Điêu Thuyền", "Ilumia"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Hồ Quang Đạn Choáng (S2 Người)", "SKILL_2", "SWIPE_TO_ENEMY", 130L, "Làm choáng mục tiêu từ xa"),
                ComboGestureStep("2. Hồ Quang Lực (S1 Người)", "SKILL_1", "SWIPE_TO_ENEMY", 130L, "Nổ sát thương phép diện rộng"),
                ComboGestureStep("3. Biến Hình Cửu Vĩ Hồ (S3)", "SKILL_3", "SWIPE_TO_ENEMY", 160L, "Lướt áp sát miễn thương"),
                ComboGestureStep("4. Cào Nội Tại + Lướt S2 Cáo", "SKILL_2", "SWIPE_TO_ENEMY", 130L, "Lướt qua mục tiêu mở khóa Vũ Điệu Yêu Hồ"),
                ComboGestureStep("5. Thả Cầu Yêu Hồ & Lùi Về Người", "SKILL_3", "SWIPE_REVERSE_ENEMY", 140L, "Rút lui an toàn về dạng Người")
            )
        ),

        HeroMechanicsSpec(
            id = "VALHEIN",
            heroName = "Valhein (Thợ Săn Ác Quỷ)",
            role = HeroRole.MARKSMAN,
            recommendedLane = AovLane.BOT_DRAGON,
            tierMeta = "A+ (Xạ Thủ Đè Đường & Làm Choáng)",
            recommendedSpell = "Tốc Biến / Cấp Cứu",
            mechanicsSummaryVi = "Thả Diều Khóa Mục Tiêu: Giữ chuỗi Đánh thường tích tốc chạy nội tại Ám Khí -> Khóa Chiêu 2 (Lời Nguyền Tử Vong làm choáng chắc chắn) -> Chiêu 1 (Chuyến Săn Mạo Hiểm) -> Khi sát thủ địch lao vào mặt, bấm ngay Chiêu 3 (Bão Đạn 6 viên) ở cự ly gần để sốc sát thương ngược.",
            whenToFightVi = "Đánh cực mạnh từ Cấp 1 đến Phút thứ 10. Chủ động cấu máu đối phương mỗi khi có Phi Tiêu Vàng (Làm choáng).",
            whenToRecallVi = "Biến về khi Máu < 25% và phép Cấp Cứu đang hồi.",
            coreItems = listOf("Giày Du Mục", "Kiếm Fafnir", "Đao Truy Hồn", "Rìu Hyoga", "Cung Tà Ma", "Giáp Hộ Mệnh"),
            situationalCounterItems = mapOf(
                "Địch nhiều sát thủ" to "Rìu Hyoga + Khiên Thất Truyền (vừa làm chậm vừa trâu)",
                "Địch nhiều Đỡ đòn" to "Kiếm Muramasa"
            ),
            counteredByHeroes = listOf("Elsu", "Stuart", "Yue"),
            strongAgainstHeroes = listOf("Hayate", "Nakroth", "Kaine", "Murad"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Khóa Phi Tiêu Vàng Choáng (S2)", "SKILL_2", "TAP", 110L, "Làm choáng mục tiêu ngay lập tức"),
                ComboGestureStep("2. Bão Đạn Cự Ly Gần (S3)", "SKILL_3", "SWIPE_TO_ENEMY", 140L, "Bắn 6 viên đạn cường hóa tốc đánh"),
                ComboGestureStep("3. Phi Tiêu Đỏ (S1)", "SKILL_1", "TAP", 100L, "Ném phi tiêu nổ diện rộng"),
                ComboGestureStep("4. Vừa Lùi Vừa Bắn (Hit & Run)", "AA_HERO", "TAP", 100L, "Kéo Joystick giữ khoảng cách và bắn thường")
            )
        ),

        HeroMechanicsSpec(
            id = "THANE",
            heroName = "Thane (Bảo Kiếm Huyền Thoại - Trợ Thủ)",
            role = HeroRole.SUPPORT,
            recommendedLane = AovLane.SUPPORT_ROAM,
            tierMeta = "S (Đỡ Đòn Mở Giao Tranh & Đẩy Lùi)",
            recommendedSpell = "Tốc Biến / Cấp Cứu",
            mechanicsSummaryVi = "Đẩy Ngược Về Đồng Đội: Nấp bụi cỏ hoặc Tốc Biến ra sau lưng địch -> Vuốt Chiêu 1 (Xung Phong) đẩy gom địch về phía Xạ thủ nhà -> Bấm Chiêu 2 (Gươm Thần) dậm nhảy hất tung -> Khi địch dưới 30% máu, bổ Chiêu 3 (Excalibur) gây sát thương chuẩn diện rộng.",
            whenToFightVi = "Mở giao tranh khi có Xạ thủ/Pháp sư đồng minh đứng trong bán kính 5m phía sau. Ưu tiên dùng S1 đẩy Sát thủ địch ra xa khi chúng lao vào bắt Xạ thủ nhà.",
            whenToRecallVi = "Chỉ biến về khi Máu < 15% và Nội tại Quyền Năng Ma Thuật (tự hồi 24% máu) đang trong thời gian hồi 75 giây.",
            coreItems = listOf("Phụ Trợ Hệ Thủy", "Giày Kiên Cường", "Khiên Huyền Thoại", "Huân Chương Troy", "Giáp Thống Khổ", "Giáp Hộ Mệnh"),
            situationalCounterItems = mapOf(
                "Xạ thủ địch bắn tay mạnh (Valhein, Yorn)" to "Giáp Thống Khổ + Khiên Thất Truyền",
                "Địch nhiều khống chế" to "Phụ Trợ Giải Khống Chế (Đại Địa Mở Trói)"
            ),
            counteredByHeroes = listOf("Hayate", "Lauriel", "Florentino", "Maloch"),
            strongAgainstHeroes = listOf("Nakroth", "Ngộ Không", "Kriknak", "Natalya"),
            signatureComboSteps = listOf(
                ComboGestureStep("1. Gươm Thần Áp Sát Hất Tung (S2)", "SKILL_2", "SWIPE_TO_ENEMY", 150L, "Nhảy làm chậm và hất tung mục tiêu"),
                ComboGestureStep("2. Xung Phong Đẩy Gom (S1)", "SKILL_1", "SWIPE_REVERSE_ENEMY", 220L, "Đẩy lùi mục tiêu về phía đồng đội"),
                ComboGestureStep("3. Excalibur Sát Thương Chuẩn (S3)", "SKILL_3", "SWIPE_TO_ENEMY", 180L, "Bổ kiếm chuẩn kết liễu mục tiêu thấp máu")
            )
        )
    )

    val TOP_META_BANS: List<Pair<String, String>> = listOf(
        "Aya" to "Cấm ưu tiên #1: Leo đầu Sát thủ/Đấu sĩ tạo giáp ảo cực khó chịu, phá vỡ mọi phép tính sát thương.",
        "Grakk" to "Cấm ưu tiên #2: Chiêu 2 Xích Hút kéo trúng là mất chủ lực ngay lập tức.",
        "Kaine" to "Cấm ưu tiên #3: Tàng hình toàn bản đồ, ép Xạ thủ/Pháp sư không dám rời trụ.",
        "Aleister / Arum" to "Cấm ưu tiên #4 (Khi bạn chơi Florentino / Nakroth / Hayate): Khóa cứng không giải được bằng Thanh Tẩy.",
        "Florentino" to "Cấm ưu tiên #5: Khả năng cân 2-3 ở đường Tà Thần nếu rơi vào tay đối thủ.",
        "Elsu" to "Cấm ưu tiên #6: Cấu rỉa Viễn Trình Kích từ ngoài tầm nhìn màn hình."
    )

    fun getHeroSpecByIdOrRole(heroId: String?, roleName: String): HeroMechanicsSpec {
        if (!heroId.isNullOrBlank()) {
            ALL_HERO_SPECS.firstOrNull { it.id == heroId }?.let { return it }
        }
        val role = runCatching { HeroRole.valueOf(roleName) }.getOrDefault(HeroRole.MARKSMAN)
        return ALL_HERO_SPECS.firstOrNull { it.role == role } ?: ALL_HERO_SPECS.first()
    }

    /**
     * Phân tích đội hình địch và đường đi để gợi ý Cấm/Chọn & Lên đồ khắc chế chuẩn Meta
     */
    fun analyzeDraftAndMeta(
        selectedLane: AovLane,
        enemyCompositionTypes: Set<String>
    ): DraftRecommendation {
        val laneHeroes = ALL_HERO_SPECS.filter { it.recommendedLane == selectedLane }
            .ifEmpty { ALL_HERO_SPECS }

        val counterAdvice = mutableListOf<String>()
        if ("HEAL_TANK" in enemyCompositionTypes) {
            counterAdvice.add("Địch có Hồi máu mạnh (Florentino/Taara/Helen): Bot sẽ tự động ưu tiên mua Đao Truy Hồn / Sách Truy Hồn ở món thứ 2.")
        }
        if ("HARD_CC" in enemyCompositionTypes) {
            counterAdvice.add("Địch nhiều Khống chế cứng (Aleister/Gildur/Grakk): Bắt buộc lên Giày Kiên Cường đầu tiên + giữ phép Thanh Tẩy.")
        }
        if ("ASSASSIN_BURST" in enemyCompositionTypes) {
            counterAdvice.add("Địch có Sát thủ Tàng hình/Sốc đam (Kaine/Ngộ Không/Paine): Bot tự lùi về ôm trụ ngay khi Minimap mất dấu (MIA >= 1) và lên Khiên Thất Truyền / Quả Cầu Băng Sương.")
        }
        if ("POKE_MAGE" in enemyCompositionTypes) {
            counterAdvice.add("Địch cấu rỉa tầm xa (Yue/Elsu/Iggy): Kích hoạt chế độ Lách Zíc-zắc 90° liên tục và lên Huân Chương Troy.")
        }
        if (counterAdvice.isEmpty()) {
            counterAdvice.add("Đội hình địch cân bằng: Lên bộ trang bị Chuẩn Sát Thương Tối Đa và tự đổi Giáp Hộ Mệnh -> Liềm Đoạt Mệnh ở phút 14+.")
        }

        return DraftRecommendation(
            priorityBans = TOP_META_BANS,
            bestPicksForLane = laneHeroes,
            counterBuildAdvice = counterAdvice
        )
    }
}
