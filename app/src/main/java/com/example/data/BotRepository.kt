package com.example.data

import com.example.engine.MicroBotBrain
import kotlinx.coroutines.flow.Flow

class BotRepository(private val botDao: BotDao) {
    val allProfiles: Flow<List<BotProfileEntity>> = botDao.getAllProfiles()

    suspend fun insert(profile: BotProfileEntity): Long = botDao.insertProfile(profile)

    suspend fun update(profile: BotProfileEntity) = botDao.updateProfile(profile)

    suspend fun deleteById(id: Int) = botDao.deleteProfileById(id)

    suspend fun seedDefaultProfilesIfNeeded() {
        if (botDao.getCount() == 0) {
            val wWarrior = MicroBotBrain.createPresetWeights(HeroRole.WARRIOR)
            val wAssassin = MicroBotBrain.createPresetWeights(HeroRole.ASSASSIN)
            val wMarksman = MicroBotBrain.createPresetWeights(HeroRole.MARKSMAN)
            val wMage = MicroBotBrain.createPresetWeights(HeroRole.MAGE)

            botDao.insertProfile(
                BotProfileEntity(
                    name = "Florentino - Múa Nhặt Hoa Đường Caesar",
                    selectedHeroId = "FLORENTINO",
                    assignedLane = "TOP_CAESAR",
                    heroRole = HeroRole.WARRIOR.name,
                    brainMode = BrainMode.HYBRID.name,
                    retreatHpPercent = 22,
                    recallManaPercent = 10,
                    diveTowerEnemyHpPercent = 18,
                    kitingDistance = 145,
                    minimapMiaAlertCount = 2,
                    targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                    comboSequence = "S1 -> AA -> S2 -> Nhặt Hoa 120° -> S3",
                    enemyCompTagsCsv = "HEAL_TANK,HARD_CC",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = true,
                    weightsCsv = MicroBotBrain.weightsToCsv(wWarrior),
                    trainedEpochs = 800,
                    lastAccuracyPercent = 96.2f
                )
            )
            botDao.insertProfile(
                BotProfileEntity(
                    name = "Nakroth - Lướt Ngược S2 Đi Rừng & Đẩy Lẻ",
                    selectedHeroId = "NAKROTH",
                    assignedLane = "JUNGLE_CORE",
                    heroRole = HeroRole.ASSASSIN.name,
                    brainMode = BrainMode.HYBRID.name,
                    retreatHpPercent = 24,
                    recallManaPercent = 16,
                    diveTowerEnemyHpPercent = 20,
                    kitingDistance = 135,
                    minimapMiaAlertCount = 3,
                    targetPriority = TargetPriority.CARRY_MARKSMAN_MAGE.name,
                    comboSequence = "S2 Ngược -> AA -> S1 -> S3 -> S1 Lùi",
                    enemyCompTagsCsv = "POKE_MAGE,HARD_CC",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = true,
                    weightsCsv = MicroBotBrain.weightsToCsv(wAssassin),
                    trainedEpochs = 800,
                    lastAccuracyPercent = 95.4f
                )
            )
            botDao.insertProfile(
                BotProfileEntity(
                    name = "Hayate - Xạ Thủ Thả Diều Sát Thương Chuẩn",
                    selectedHeroId = "HAYATE",
                    assignedLane = "BOT_DRAGON",
                    heroRole = HeroRole.MARKSMAN.name,
                    brainMode = BrainMode.HYBRID.name,
                    retreatHpPercent = 28,
                    recallManaPercent = 15,
                    diveTowerEnemyHpPercent = 12,
                    kitingDistance = 265,
                    minimapMiaAlertCount = 2,
                    targetPriority = TargetPriority.NEAREST_ENEMY.name,
                    comboSequence = "S1 Song Song -> AA -> S2 Lướt -> S3 + Thanh Tẩy",
                    enemyCompTagsCsv = "HEAL_TANK,ASSASSIN_BURST",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = true,
                    weightsCsv = MicroBotBrain.weightsToCsv(wMarksman),
                    trainedEpochs = 750,
                    lastAccuracyPercent = 95.8f
                )
            )
            botDao.insertProfile(
                BotProfileEntity(
                    name = "Tulen / Liliana - Pháp Sư Đường Giữa Đảo Gank",
                    selectedHeroId = "TULEN",
                    assignedLane = "MID_ROYAL",
                    heroRole = HeroRole.MAGE.name,
                    brainMode = BrainMode.MICRO_AI.name,
                    retreatHpPercent = 28,
                    recallManaPercent = 18,
                    diveTowerEnemyHpPercent = 14,
                    kitingDistance = 245,
                    minimapMiaAlertCount = 2,
                    targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                    comboSequence = "S2 Áp Sát -> S1 3 Tia -> S2 Lùi -> S3 Lôi Điểu",
                    enemyCompTagsCsv = "HEAL_TANK,POKE_MAGE",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = false,
                    weightsCsv = MicroBotBrain.weightsToCsv(wMage),
                    trainedEpochs = 900,
                    lastAccuracyPercent = 96.5f
                )
            )
        }
    }
}
