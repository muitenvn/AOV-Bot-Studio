package com.example.engine

import com.example.data.BotProfileEntity
import com.example.data.HeroRole
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class MinionUnit(
    val id: Int,
    val isAlly: Boolean,
    var x: Float,
    var y: Float,
    var hp: Float,
    val maxHp: Float = 420f
)

data class ProjectileUnit(
    val id: Int,
    val isAlly: Boolean,
    val isSkillshot: Boolean,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val radius: Float = if (isSkillshot) 18f else 9f,
    val colorHex: Long = if (isAlly) 0xFF00E5FF else 0xFFFF5252
)

data class FloatingTextItem(
    val id: Int,
    val text: String,
    var x: Float,
    var y: Float,
    val colorHex: Long,
    var ticksLeft: Int = 38
)

data class ArenaSnapshot(
    val tickCount: Long = 0L,
    val isPaused: Boolean = false,
    // Bot Hero
    val botX: Float = 270f,
    val botY: Float = 400f,
    val botTargetX: Float = 500f,
    val botTargetY: Float = 310f,
    val botHp: Float = 1000f,
    val botMaxHp: Float = 1000f,
    val botMana: Float = 500f,
    val botMaxMana: Float = 500f,
    val botGold: Int = 850,
    val botKills: Int = 0,
    val botDeaths: Int = 0,
    val botItemsOwned: List<String> = listOf("Giày Kiên Cường"),
    // Cooldowns (in seconds)
    val cdSkill1: Float = 0f,
    val cdSkill2: Float = 0f,
    val cdUlt: Float = 0f,
    val cdHeal: Float = 0f,
    val cdFlicker: Float = 0f,
    // Enemy Hero
    val enemyX: Float = 720f,
    val enemyY: Float = 225f,
    val enemyHp: Float = 1000f,
    val enemyMaxHp: Float = 1000f,
    val enemyStunSec: Float = 0f,
    // Towers
    val allyTowerHp: Float = 2600f,
    val allyTowerMaxHp: Float = 2600f,
    val enemyTowerHp: Float = 2600f,
    val enemyTowerMaxHp: Float = 2600f,
    // Jungle Camp (Bùa Đỏ)
    val jungleX: Float = 365f,
    val jungleY: Float = 185f,
    val jungleHp: Float = 750f,
    val jungleMaxHp: Float = 750f,
    val jungleRespawnSec: Float = 0f,
    // Dynamic units
    val minions: List<MinionUnit> = emptyList(),
    val projectiles: List<ProjectileUnit> = emptyList(),
    val floatingTexts: List<FloatingTextItem> = emptyList(),
    // Telemetry from Brain
    val lastDecision: BrainDecisionResult? = null,
    val avgDecisionMicros: Float = 1.2f,
    val peakDecisionMicros: Float = 2.8f,
    val totalDecisionsCount: Long = 0L,
    val decisionLogs: List<String> = emptyList()
)

class ArenaSimulationEngine {
    companion object {
        const val WORLD_W = 1000f
        const val WORLD_H = 620f
        const val ALLY_FOUNTAIN_X = 90f
        const val ALLY_FOUNTAIN_Y = 530f
        const val ALLY_TOWER_X = 250f
        const val ALLY_TOWER_Y = 420f
        const val ENEMY_TOWER_X = 750f
        const val ENEMY_TOWER_Y = 200f
        const val ENEMY_FOUNTAIN_X = 910f
        const val ENEMY_FOUNTAIN_Y = 90f
        const val TOWER_RANGE = 175f

        val SHOP_ITEMS = listOf(
            "Giày Kiên Cường" to 700,
            "Thương Xuyên Phá" to 1400,
            "Thánh Kiếm" to 2100,
            "Phức Hợp Kiếm" to 2900,
            "Cung Tà Ma" to 3800,
            "Giáp Hộ Mệnh" to 4800
        )
    }

    private var nextEntityId = 1
    private var botAttackCd = 0f
    private var enemyAttackCd = 0f
    private var enemySkillshotCd = 2.5f
    private var allyTowerCd = 0f
    private var enemyTowerCd = 0f
    private var minionWaveTimer = 0.5f

    fun stepSimulation(
        current: ArenaSnapshot,
        profile: BotProfileEntity,
        cachedWeights: FloatArray,
        dtSec: Float = 0.05f
    ): ArenaSnapshot {
        if (current.isPaused) return current

        val role = runCatching { HeroRole.valueOf(profile.heroRole) }.getOrDefault(HeroRole.MARKSMAN)
        var botX = current.botX
        var botY = current.botY
        var botTargetX = current.botTargetX
        var botTargetY = current.botTargetY
        var botHp = current.botHp
        var botMana = (current.botMana + 14f * dtSec).coerceAtMost(current.botMaxMana)
        var botGold = current.botGold + (if (current.tickCount % 20L == 0L) 4 else 0)
        var botKills = current.botKills
        var botDeaths = current.botDeaths

        var enemyX = current.enemyX
        var enemyY = current.enemyY
        var enemyHp = current.enemyHp
        var enemyStunSec = max(0f, current.enemyStunSec - dtSec)

        var allyTowerHp = current.allyTowerHp
        var enemyTowerHp = current.enemyTowerHp
        var jungleHp = current.jungleHp
        var jungleRespawnSec = current.jungleRespawnSec

        var cdS1 = max(0f, current.cdSkill1 - dtSec)
        var cdS2 = max(0f, current.cdSkill2 - dtSec)
        var cdUlt = max(0f, current.cdUlt - dtSec)
        var cdHeal = max(0f, current.cdHeal - dtSec)
        var cdFlicker = max(0f, current.cdFlicker - dtSec)

        botAttackCd = max(0f, botAttackCd - dtSec)
        enemyAttackCd = max(0f, enemyAttackCd - dtSec)
        enemySkillshotCd = max(0f, enemySkillshotCd - dtSec)
        allyTowerCd = max(0f, allyTowerCd - dtSec)
        enemyTowerCd = max(0f, enemyTowerCd - dtSec)
        minionWaveTimer -= dtSec

        val minions = current.minions.map { it.copy() }.toMutableList()
        val projectiles = current.projectiles.map { it.copy() }.toMutableList()
        val floatingTexts = current.floatingTexts.mapNotNull {
            val nextTicks = it.ticksLeft - 1
            if (nextTicks > 0) it.copy(y = it.y - 0.8f, ticksLeft = nextTicks) else null
        }.toMutableList()
        val logs = current.decisionLogs.toMutableList()

        // 1. Sinh đợt lính mỗi 9 giây nếu dưới 6 con
        if (minionWaveTimer <= 0f && minions.size < 6) {
            minionWaveTimer = 9.0f
            minions.add(MinionUnit(nextEntityId++, true, ALLY_TOWER_X + 25f, ALLY_TOWER_Y - 12f, 420f))
            minions.add(MinionUnit(nextEntityId++, true, ALLY_TOWER_X + 5f, ALLY_TOWER_Y - 28f, 420f))
            minions.add(MinionUnit(nextEntityId++, false, ENEMY_TOWER_X - 25f, ENEMY_TOWER_Y + 12f, 420f))
            minions.add(MinionUnit(nextEntityId++, false, ENEMY_TOWER_X - 5f, ENEMY_TOWER_Y + 28f, 420f))
        }

        // Hồi sinh quái rừng
        if (jungleHp <= 0f) {
            jungleRespawnSec -= dtSec
            if (jungleRespawnSec <= 0f) {
                jungleHp = current.jungleMaxHp
                floatingTexts.add(FloatingTextItem(nextEntityId++, "Bùa Đỏ Xuất Hiện!", current.jungleX, current.jungleY - 20f, 0xFF00E676))
            }
        }

        // 2. Kiểm tra đạn định hướng địch đang bay về phía Bot
        val dangerousSkillshot = projectiles.firstOrNull { proj ->
            !proj.isAlly && proj.isSkillshot &&
                MicroBotBrain.distance(proj.x, proj.y, botX, botY) < 175f
        }
        val incomingThreat = if (dangerousSkillshot != null) 1.0f else 0.0f

        val distBotToEnemy = MicroBotBrain.distance(botX, botY, enemyX, enemyY)
        val distBotToAllyTower = MicroBotBrain.distance(botX, botY, ALLY_TOWER_X, ALLY_TOWER_Y)
        val distEnemyToEnemyTower = MicroBotBrain.distance(enemyX, enemyY, ENEMY_TOWER_X, ENEMY_TOWER_Y)
        val enemyUnderTowerFlag = if (enemyTowerHp > 0f && distEnemyToEnemyTower <= TOWER_RANGE) 1.0f else 0.0f

        var readySkillsCount = 0
        if (cdS1 <= 0f) readySkillsCount++
        if (cdS2 <= 0f) readySkillsCount++
        if (cdUlt <= 0f) readySkillsCount++
        val skillsReadyRatio = readySkillsCount / 3f

        val enemyMinionList = minions.filter { !it.isAlly }

        // 3. Gọi Bộ não Bot (Đo thời gian thực nanoTime)
        val inputState = TacticalInputState(
            botHpRatio = botHp / current.botMaxHp,
            botManaRatio = botMana / current.botMaxMana,
            enemyHpRatio = enemyHp / current.enemyMaxHp,
            distToEnemyNorm = (distBotToEnemy / 600f).coerceIn(0f, 1f),
            distToAllyTowerNorm = (distBotToAllyTower / 600f).coerceIn(0f, 1f),
            enemyUnderTower = enemyUnderTowerFlag,
            skillsReadyRatio = skillsReadyRatio,
            incomingProjectileThreat = incomingThreat,
            enemyMinionCount = enemyMinionList.size,
            jungleMonsterAlive = jungleHp > 0f
        )

        val decision = MicroBotBrain.decide(inputState, profile, cachedWeights)

        // Ghi nhật ký nếu đổi hành động hoặc mỗi 35 nhịp
        if (current.lastDecision?.action != decision.action || current.tickCount % 35L == 0L) {
            val formattedUs = "%.1f µs".format(java.util.Locale.US, decision.executionMicros)
            val logEntry = "[${formattedUs}] ${decision.action.titleVi} — ${decision.reasonVi}"
            logs.add(0, logEntry)
            if (logs.size > 14) logs.removeAt(logs.lastIndex)
        }

        // 4. Thực thi hành động của Bot trên Sân Đấu
        val moveSpeed = role.baseSpeed * 1.45f
        val attackRange = max(125f, profile.kitingDistance.toFloat())
        val bonusDamage = 1f + (current.botItemsOwned.size * 0.14f)

        when (decision.action) {
            BotTacticalAction.DODGE_SKILLSHOT -> {
                if (dangerousSkillshot != null) {
                    // Tính vector vuông góc 90 độ với hướng bay của viên đạn
                    val speedLen = sqrt(dangerousSkillshot.vx * dangerousSkillshot.vx + dangerousSkillshot.vy * dangerousSkillshot.vy).coerceAtLeast(1f)
                    val perpX = -dangerousSkillshot.vy / speedLen
                    val perpY = dangerousSkillshot.vx / speedLen
                    botTargetX = (botX + perpX * 110f).coerceIn(60f, WORLD_W - 60f)
                    botTargetY = (botY + perpY * 110f).coerceIn(60f, WORLD_H - 60f)
                    botX = (botX + perpX * moveSpeed * 1.65f).coerceIn(60f, WORLD_W - 60f)
                    botY = (botY + perpY * moveSpeed * 1.65f).coerceIn(60f, WORLD_H - 60f)
                    if (current.tickCount % 12L == 0L) {
                        floatingTexts.add(FloatingTextItem(nextEntityId++, "LÁCH 90°!", botX, botY - 26f, 0xFFFFEA00))
                    }
                }
            }

            BotTacticalAction.RETREAT_HEAL -> {
                botTargetX = ALLY_FOUNTAIN_X + 35f
                botTargetY = ALLY_FOUNTAIN_Y - 25f
                // Kích hoạt phép bổ trợ Hồi Máu nếu có
                if (cdHeal <= 0f && botHp < current.botMaxHp * 0.65f) {
                    cdHeal = 14f
                    botHp = (botHp + 240f).coerceAtMost(current.botMaxHp)
                    floatingTexts.add(FloatingTextItem(nextEntityId++, "+240 HỒI MÁU", botX, botY - 28f, 0xFF00E676))
                }
                // Kích hoạt Tốc Biến nếu địch áp sát nguy hiểm
                if (cdFlicker <= 0f && distBotToEnemy < 160f && botHp < current.botMaxHp * 0.28f) {
                    cdFlicker = 22f
                    val angle = atan2(ALLY_FOUNTAIN_Y - botY, ALLY_FOUNTAIN_X - botX)
                    botX = (botX + cos(angle) * 125f).coerceIn(50f, WORLD_W - 50f)
                    botY = (botY + sin(angle) * 125f).coerceIn(50f, WORLD_H - 50f)
                    floatingTexts.add(FloatingTextItem(nextEntityId++, "TỐC BIẾN!", botX, botY - 30f, 0xFF00E5FF))
                }
                val pos = stepTowards(botX, botY, botTargetX, botTargetY, moveSpeed * 1.15f)
                botX = pos.first
                botY = pos.second
                // Hồi máu nhanh tại Tế Đàn
                if (MicroBotBrain.distance(botX, botY, ALLY_FOUNTAIN_X, ALLY_FOUNTAIN_Y) < 110f) {
                    botHp = (botHp + 95f * dtSec * 3f).coerceAtMost(current.botMaxHp)
                    botMana = (botMana + 80f * dtSec * 3f).coerceAtMost(current.botMaxMana)
                }
            }

            BotTacticalAction.ATTACK_COMBO -> {
                botTargetX = enemyX
                botTargetY = enemyY
                if (distBotToEnemy > attackRange * 0.85f) {
                    val pos = stepTowards(botX, botY, enemyX, enemyY, moveSpeed * 1.1f)
                    botX = pos.first
                    botY = pos.second
                }
                // Tung chuỗi Combo kỹ năng theo cấu hình của người dùng
                if (distBotToEnemy <= attackRange + 55f) {
                    if (cdS2 <= 0f && botMana >= 45f) {
                        cdS2 = 6.5f
                        botMana -= 45f
                        enemyStunSec = 1.1f
                        projectiles.add(
                             createProjectile(true, true, botX, botY, enemyX, enemyY, 155f * bonusDamage, 0xFFFFEA00)
                        )
                        floatingTexts.add(FloatingTextItem(nextEntityId++, "CHIÊU 2 CHOÁNG!", botX, botY - 25f, 0xFFFFEA00))
                    } else if (cdUlt <= 0f && botMana >= 80f) {
                        cdUlt = 12.0f
                        botMana -= 80f
                        projectiles.add(
                            createProjectile(true, true, botX, botY, enemyX, enemyY, 275f * bonusDamage, 0xFFFFB300)
                        )
                        floatingTexts.add(FloatingTextItem(nextEntityId++, "ULTIMATE!", botX, botY - 32f, 0xFFFFB300))
                    } else if (cdS1 <= 0f && botMana >= 35f) {
                        cdS1 = 4.2f
                        botMana -= 35f
                        projectiles.add(
                            createProjectile(true, true, botX, botY, enemyX, enemyY, 135f * bonusDamage, 0xFF00E5FF)
                        )
                        floatingTexts.add(FloatingTextItem(nextEntityId++, "CHIÊU 1!", botX, botY - 22f, 0xFF00E5FF))
                    } else if (botAttackCd <= 0f) {
                        botAttackCd = 0.65f
                        projectiles.add(
                            createProjectile(true, false, botX, botY, enemyX, enemyY, 78f * bonusDamage, 0xFF00E5FF)
                        )
                    }
                }
            }

            BotTacticalAction.KITE_POKE -> {
                val desiredDist = attackRange * 0.92f
                if (distBotToEnemy < desiredDist - 25f) {
                    // Lùi giữ khoảng cách thả diều (Hit & Run)
                    val pos = stepTowards(botX, botY, ALLY_TOWER_X, ALLY_TOWER_Y, moveSpeed)
                    botX = pos.first
                    botY = pos.second
                    botTargetX = ALLY_TOWER_X
                    botTargetY = ALLY_TOWER_Y
                } else if (distBotToEnemy > desiredDist + 30f) {
                    val pos = stepTowards(botX, botY, enemyX, enemyY, moveSpeed)
                    botX = pos.first
                    botY = pos.second
                    botTargetX = enemyX
                    botTargetY = enemyY
                }
                if (distBotToEnemy <= attackRange + 35f) {
                    if (cdS1 <= 0f && botMana >= 35f) {
                        cdS1 = 4.2f
                        botMana -= 35f
                        projectiles.add(
                            createProjectile(true, true, botX, botY, enemyX, enemyY, 130f * bonusDamage, 0xFF00E5FF)
                        )
                    } else if (botAttackCd <= 0f) {
                        botAttackCd = 0.65f
                        projectiles.add(
                            createProjectile(true, false, botX, botY, enemyX, enemyY, 75f * bonusDamage, 0xFF00E5FF)
                        )
                    }
                }
            }

            BotTacticalAction.PUSH_LANE -> {
                val targetMinion = enemyMinionList.minByOrNull { MicroBotBrain.distance(botX, botY, it.x, it.y) }
                val tx = targetMinion?.x ?: ENEMY_TOWER_X
                val ty = targetMinion?.y ?: ENEMY_TOWER_Y
                botTargetX = tx
                botTargetY = ty
                val distToTarget = MicroBotBrain.distance(botX, botY, tx, ty)

                // Chỉ bắn trụ địch khi có lính đồng minh đỡ trụ hoặc trụ yếu
                val allyMinionsUnderEnemyTower = minions.any {
                    it.isAlly && MicroBotBrain.distance(it.x, it.y, ENEMY_TOWER_X, ENEMY_TOWER_Y) <= TOWER_RANGE
                }
                if (targetMinion == null && !allyMinionsUnderEnemyTower && enemyTowerHp > 0f) {
                    // Đứng ngoài vòng trụ chờ lính lên
                    val safeX = ENEMY_TOWER_X - 155f
                    val safeY = ENEMY_TOWER_Y + 85f
                    val pos = stepTowards(botX, botY, safeX, safeY, moveSpeed)
                    botX = pos.first
                    botY = pos.second
                } else if (distToTarget > attackRange * 0.85f) {
                    val pos = stepTowards(botX, botY, tx, ty, moveSpeed)
                    botX = pos.first
                    botY = pos.second
                } else if (botAttackCd <= 0f) {
                    botAttackCd = 0.65f
                    if (targetMinion != null) {
                        targetMinion.hp -= 95f * bonusDamage
                        if (targetMinion.hp <= 0f) {
                            botGold += 55
                            floatingTexts.add(FloatingTextItem(nextEntityId++, "+55 VÀNG", targetMinion.x, targetMinion.y - 15f, 0xFFFFD54F))
                        }
                    } else if (enemyTowerHp > 0f) {
                        enemyTowerHp = max(0f, enemyTowerHp - 85f * bonusDamage)
                        floatingTexts.add(FloatingTextItem(nextEntityId++, "-${(85f * bonusDamage).toInt()} TRỤ", ENEMY_TOWER_X, ENEMY_TOWER_Y - 25f, 0xFFFFB300))
                    }
                }
            }

            BotTacticalAction.FARM_JUNGLE -> {
                if (jungleHp > 0f) {
                    botTargetX = current.jungleX
                    botTargetY = current.jungleY
                    val distJungle = MicroBotBrain.distance(botX, botY, current.jungleX, current.jungleY)
                    if (distJungle > 110f) {
                        val pos = stepTowards(botX, botY, current.jungleX, current.jungleY, moveSpeed)
                        botX = pos.first
                        botY = pos.second
                    } else if (botAttackCd <= 0f) {
                        botAttackCd = 0.6f
                        jungleHp -= 115f * bonusDamage
                        if (jungleHp <= 0f) {
                            jungleRespawnSec = 15f
                            botGold += 120
                            botHp = (botHp + 180f).coerceAtMost(current.botMaxHp)
                            floatingTexts.add(FloatingTextItem(nextEntityId++, "+120 VÀNG & BÙA ĐỎ!", current.jungleX, current.jungleY - 24f, 0xFFFF5252))
                        }
                    }
                } else {
                    val pos = stepTowards(botX, botY, 500f, 310f, moveSpeed)
                    botX = pos.first
                    botY = pos.second
                }
            }
        }

        // 5. Cập nhật AI Tướng Địch (để trận đấu diễn ra sinh động)
        if (enemyStunSec <= 0f) {
            val enemyDistToBot = MicroBotBrain.distance(enemyX, enemyY, botX, botY)
            if (enemyHp < current.enemyMaxHp * 0.22f) {
                val pos = stepTowards(enemyX, enemyY, ENEMY_FOUNTAIN_X, ENEMY_FOUNTAIN_Y, 3.3f)
                enemyX = pos.first
                enemyY = pos.second
                if (MicroBotBrain.distance(enemyX, enemyY, ENEMY_FOUNTAIN_X, ENEMY_FOUNTAIN_Y) < 110f) {
                    enemyHp = (enemyHp + 110f * dtSec * 3f).coerceAtMost(current.enemyMaxHp)
                }
            } else {
                val desiredX = if (enemyTowerHp > 0f) 560f else 680f
                val desiredY = if (enemyTowerHp > 0f) 285f else 230f
                if (enemyDistToBot > 240f) {
                    val pos = stepTowards(enemyX, enemyY, desiredX, desiredY, 3.0f)
                    enemyX = pos.first
                    enemyY = pos.second
                }
                // Địch bắn chiêu định hướng mỗi vài giây để thử phản xạ né của Bot
                if (enemySkillshotCd <= 0f && enemyDistToBot < 360f) {
                    enemySkillshotCd = 3.8f
                    projectiles.add(
                        createProjectile(false, true, enemyX, enemyY, botX, botY, 145f, 0xFFFF5252)
                    )
                } else if (enemyAttackCd <= 0f && enemyDistToBot < 230f) {
                    enemyAttackCd = 0.85f
                    projectiles.add(
                        createProjectile(false, false, enemyX, enemyY, botX, botY, 68f, 0xFFFF8A80)
                    )
                }
            }
        }

        // 6. Cập nhật Lính hai phe và Trụ bắn
        for (m in minions) {
            if (m.hp <= 0f) continue
            val opposingMinion = minions.firstOrNull {
                it.hp > 0f && it.isAlly != m.isAlly && MicroBotBrain.distance(m.x, m.y, it.x, it.y) < 65f
            }
            if (opposingMinion != null) {
                opposingMinion.hp -= 28f * dtSec
            } else {
                val goalX = if (m.isAlly) ENEMY_TOWER_X else ALLY_TOWER_X
                val goalY = if (m.isAlly) ENEMY_TOWER_Y else ALLY_TOWER_Y
                if (MicroBotBrain.distance(m.x, m.y, goalX, goalY) > 55f) {
                    val pos = stepTowards(m.x, m.y, goalX, goalY, 1.9f)
                    m.x = pos.first
                    m.y = pos.second
                } else {
                    if (m.isAlly && enemyTowerHp > 0f) enemyTowerHp = max(0f, enemyTowerHp - 22f * dtSec)
                    if (!m.isAlly && allyTowerHp > 0f) allyTowerHp = max(0f, allyTowerHp - 22f * dtSec)
                }
            }
        }
        minions.removeAll { it.hp <= 0f }

        // Trụ địch bắn nếu không có lính đồng minh hoặc Bot đang đánh tướng địch trong trụ
        if (enemyTowerHp > 0f && enemyTowerCd <= 0f) {
            val allyMinionInTower = minions.firstOrNull {
                it.isAlly && MicroBotBrain.distance(it.x, it.y, ENEMY_TOWER_X, ENEMY_TOWER_Y) <= TOWER_RANGE
            }
            val botInEnemyTower = MicroBotBrain.distance(botX, botY, ENEMY_TOWER_X, ENEMY_TOWER_Y) <= TOWER_RANGE
            if (botInEnemyTower && (allyMinionInTower == null || decision.action == BotTacticalAction.ATTACK_COMBO)) {
                enemyTowerCd = 1.15f
                projectiles.add(createProjectile(false, false, ENEMY_TOWER_X, ENEMY_TOWER_Y, botX, botY, 175f, 0xFFFFB300))
                floatingTexts.add(FloatingTextItem(nextEntityId++, "TRỤ BẮN!", botX, botY - 20f, 0xFFFF5252))
            } else if (allyMinionInTower != null) {
                enemyTowerCd = 1.15f
                allyMinionInTower.hp -= 160f
            }
        }

        // Trụ đồng minh bắn
        if (allyTowerHp > 0f && allyTowerCd <= 0f) {
            val enemyMinionInTower = minions.firstOrNull {
                !it.isAlly && MicroBotBrain.distance(it.x, it.y, ALLY_TOWER_X, ALLY_TOWER_Y) <= TOWER_RANGE
            }
            val enemyInAllyTower = MicroBotBrain.distance(enemyX, enemyY, ALLY_TOWER_X, ALLY_TOWER_Y) <= TOWER_RANGE
            if (enemyMinionInTower != null) {
                allyTowerCd = 1.15f
                enemyMinionInTower.hp -= 165f
            } else if (enemyInAllyTower) {
                allyTowerCd = 1.15f
                projectiles.add(createProjectile(true, false, ALLY_TOWER_X, ALLY_TOWER_Y, enemyX, enemyY, 175f, 0xFF00E5FF))
            }
        }

        // 7. Cập nhật đường đạn và va chạm
        val remainingProjectiles = mutableListOf<ProjectileUnit>()
        for (p in projectiles) {
            p.x += p.vx
            p.y += p.vy
            if (p.x !in 0f..WORLD_W || p.y !in 0f..WORLD_H) continue

            if (p.isAlly) {
                val hitDist = MicroBotBrain.distance(p.x, p.y, enemyX, enemyY)
                if (hitDist <= p.radius + 20f) {
                    enemyHp -= p.damage
                    floatingTexts.add(
                        FloatingTextItem(nextEntityId++, "-${p.damage.toInt()}", enemyX, enemyY - 18f, 0xFF00E5FF)
                    )
                    if (enemyHp <= 0f) {
                        botKills++
                        botGold += 300
                        enemyHp = current.enemyMaxHp
                        enemyX = ENEMY_FOUNTAIN_X - 20f
                        enemyY = ENEMY_FOUNTAIN_Y + 20f
                        floatingTexts.add(
                            FloatingTextItem(nextEntityId++, "HẠ GỤC! +300 VÀNG", botX, botY - 35f, 0xFFFFD54F)
                        )
                    }
                    continue
                }
            } else {
                val hitDist = MicroBotBrain.distance(p.x, p.y, botX, botY)
                if (hitDist <= p.radius + 18f) {
                    botHp -= p.damage
                    floatingTexts.add(
                        FloatingTextItem(nextEntityId++, "-${p.damage.toInt()}", botX, botY - 18f, 0xFFFF5252)
                    )
                    if (botHp <= 0f) {
                        botDeaths++
                        botHp = current.botMaxHp
                        botMana = current.botMaxMana
                        botX = ALLY_FOUNTAIN_X + 20f
                        botY = ALLY_FOUNTAIN_Y - 20f
                        floatingTexts.add(
                            FloatingTextItem(nextEntityId++, "HỒI SINH TẠI TẾ ĐÀN", botX, botY - 25f, 0xFFB388FF)
                        )
                    }
                    continue
                }
            }
            remainingProjectiles.add(p)
        }

        // 8. Tự động mua trang bị khi đủ vàng
        val updatedItems = current.botItemsOwned.toMutableList()
        if (profile.autoBuyEquipment && updatedItems.size < SHOP_ITEMS.size) {
            val nextShopItem = SHOP_ITEMS[updatedItems.size]
            if (botGold >= nextShopItem.second) {
                updatedItems.add(nextShopItem.first)
                floatingTexts.add(
                    FloatingTextItem(nextEntityId++, "MUA: ${nextShopItem.first}", botX, botY - 36f, 0xFFFFD54F)
                )
            }
        }

        val totalDec = current.totalDecisionsCount + 1L
        val newAvgMicros = ((current.avgDecisionMicros * min(50L, current.totalDecisionsCount)) + decision.executionMicros) /
            min(51L, totalDec).toFloat()
        val newPeakMicros = max(current.peakDecisionMicros * 0.995f, decision.executionMicros)

        return current.copy(
            tickCount = current.tickCount + 1L,
            botX = botX,
            botY = botY,
            botTargetX = botTargetX,
            botTargetY = botTargetY,
            botHp = botHp,
            botMana = botMana,
            botGold = botGold,
            botKills = botKills,
            botDeaths = botDeaths,
            botItemsOwned = updatedItems,
            cdSkill1 = cdS1,
            cdSkill2 = cdS2,
            cdUlt = cdUlt,
            cdHeal = cdHeal,
            cdFlicker = cdFlicker,
            enemyX = enemyX,
            enemyY = enemyY,
            enemyHp = enemyHp,
            enemyStunSec = enemyStunSec,
            allyTowerHp = if (allyTowerHp <= 0f) current.allyTowerMaxHp else allyTowerHp,
            enemyTowerHp = if (enemyTowerHp <= 0f) current.enemyTowerMaxHp else enemyTowerHp,
            jungleHp = jungleHp,
            jungleRespawnSec = jungleRespawnSec,
            minions = minions,
            projectiles = remainingProjectiles,
            floatingTexts = floatingTexts,
            lastDecision = decision,
            avgDecisionMicros = newAvgMicros,
            peakDecisionMicros = newPeakMicros,
            totalDecisionsCount = totalDec,
            decisionLogs = logs
        )
    }

    /**
     * Tạo các tình huống thực chiến Liên Quân để người dùng thử phản xạ Bot ngay lập tức
     */
    fun triggerScenario(current: ArenaSnapshot, scenarioType: Int): ArenaSnapshot {
        val projs = current.projectiles.toMutableList()
        val texts = current.floatingTexts.toMutableList()
        return when (scenarioType) {
            // 1: Địch bắn liên hoàn 2 chiêu định hướng về phía Bot -> Test phản xạ lách 90 độ
            1 -> {
                projs.add(createProjectile(false, true, current.enemyX, current.enemyY, current.botX, current.botY, 160f, 0xFFFF5252))
                projs.add(createProjectile(false, true, current.enemyX - 30f, current.enemyY + 25f, current.botX, current.botY, 160f, 0xFFFF5252))
                texts.add(FloatingTextItem(nextEntityId++, "THỬ THÁCH: NÉ ĐẠN ĐỊNH HƯỚNG!", current.botX, current.botY - 35f, 0xFFFFEA00))
                current.copy(projectiles = projs, floatingTexts = texts)
            }
            // 2: Địch còn 12% máu đứng sát trụ -> Test quyết định dồn combo kết liễu
            2 -> {
                texts.add(FloatingTextItem(nextEntityId++, "TÌNH HUỐNG: ĐỊCH 12% MÁU!", ENEMY_TOWER_X - 70f, ENEMY_TOWER_Y + 40f, 0xFF00E5FF))
                current.copy(
                    enemyX = ENEMY_TOWER_X - 80f,
                    enemyY = ENEMY_TOWER_Y + 45f,
                    enemyHp = current.enemyMaxHp * 0.12f,
                    botX = ENEMY_TOWER_X - 210f,
                    botY = ENEMY_TOWER_Y + 110f,
                    cdSkill1 = 0f,
                    cdSkill2 = 0f,
                    cdUlt = 0f,
                    floatingTexts = texts
                )
            }
            // 3: Bị gank bất ngờ máu tụt xuống 18% -> Test Tốc Biến + Hồi Máu rút lui
            3 -> {
                texts.add(FloatingTextItem(nextEntityId++, "BỊ GANK! MÁU CÒN 18%!", current.botX, current.botY - 35f, 0xFFFF5252))
                current.copy(
                    botHp = current.botMaxHp * 0.18f,
                    enemyX = current.botX + 115f,
                    enemyY = current.botY - 60f,
                    cdHeal = 0f,
                    cdFlicker = 0f,
                    floatingTexts = texts
                )
            }
            // 4: Đường trống sạch lính -> Test tự động xoay sang ăn Bùa Đỏ
            else -> {
                texts.add(FloatingTextItem(nextEntityId++, "ĐƯỜNG TRỐNG -> XOAY ĂN BÙA!", current.jungleX, current.jungleY - 25f, 0xFF00E676))
                current.copy(
                    minions = emptyList(),
                    jungleHp = current.jungleMaxHp,
                    enemyX = ENEMY_FOUNTAIN_X - 40f,
                    enemyY = ENEMY_FOUNTAIN_Y + 40f,
                    floatingTexts = texts
                )
            }
        }
    }

    private fun stepTowards(x: Float, y: Float, tx: Float, ty: Float, speed: Float): Pair<Float, Float> {
        val dx = tx - x
        val dy = ty - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= speed || dist < 1e-3f) return tx to ty
        return (x + (dx / dist) * speed).coerceIn(45f, WORLD_W - 45f) to
            (y + (dy / dist) * speed).coerceIn(45f, WORLD_H - 45f)
    }

    private fun createProjectile(
        isAlly: Boolean,
        isSkillshot: Boolean,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        damage: Float,
        colorHex: Long
    ): ProjectileUnit {
        val dx = toX - fromX
        val dy = toY - fromY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val speed = if (isSkillshot) 13.5f else 16.5f
        return ProjectileUnit(
            id = nextEntityId++,
            isAlly = isAlly,
            isSkillshot = isSkillshot,
            x = fromX,
            y = fromY,
            vx = (dx / dist) * speed,
            vy = (dy / dist) * speed,
            damage = damage,
            colorHex = colorHex
        )
    }
}
