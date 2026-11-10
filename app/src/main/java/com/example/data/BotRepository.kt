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
            val defaultWeightsMarksman = MicroBotBrain.createPresetWeights(HeroRole.MARKSMAN)
            val defaultWeightsAssassin = MicroBotBrain.createPresetWeights(HeroRole.ASSASSIN)
            val defaultWeightsMage = MicroBotBrain.createPresetWeights(HeroRole.MAGE)

            botDao.insertProfile(
                BotProfileEntity(
                    name = "Valhein / Hayate - Xạ Thủ Thả Diều",
                    heroRole = HeroRole.MARKSMAN.name,
                    brainMode = BrainMode.HYBRID.name,
                    retreatHpPercent = 28,
                    diveTowerEnemyHpPercent = 14,
                    kitingDistance = 260,
                    targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                    comboSequence = "S2 -> S1 -> AA -> S3",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = true,
                    weightsCsv = MicroBotBrain.weightsToCsv(defaultWeightsMarksman),
                    trainedEpochs = 500,
                    lastAccuracyPercent = 94.2f
                )
            )
            botDao.insertProfile(
                BotProfileEntity(
                    name = "Nakroth / Kriknak - Sát Thủ Săn Chủ Lực",
                    heroRole = HeroRole.ASSASSIN.name,
                    brainMode = BrainMode.RULE_FSM.name,
                    retreatHpPercent = 22,
                    diveTowerEnemyHpPercent = 20,
                    kitingDistance = 130,
                    targetPriority = TargetPriority.LOWEST_HP_HERO.name,
                    comboSequence = "S2 -> S3 -> S1 -> AA",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = true,
                    weightsCsv = MicroBotBrain.weightsToCsv(defaultWeightsAssassin),
                    trainedEpochs = 500,
                    lastAccuracyPercent = 92.8f
                )
            )
            botDao.insertProfile(
                BotProfileEntity(
                    name = "Tulen / Krixi - Pháp Sư Cấu Rỉa Đường Giữa",
                    heroRole = HeroRole.MAGE.name,
                    brainMode = BrainMode.MICRO_AI.name,
                    retreatHpPercent = 30,
                    diveTowerEnemyHpPercent = 12,
                    kitingDistance = 245,
                    targetPriority = TargetPriority.TOWER_AND_WAVE.name,
                    comboSequence = "S1 -> S2 -> S3 -> AA",
                    autoDodgeSkillshot = true,
                    autoJungleWhenLaneClear = false,
                    weightsCsv = MicroBotBrain.weightsToCsv(defaultWeightsMage),
                    trainedEpochs = 800,
                    lastAccuracyPercent = 95.0f
                )
            )
        }
    }
}
