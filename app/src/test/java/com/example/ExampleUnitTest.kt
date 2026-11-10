package com.example

import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.engine.AovHeroAndMetaDatabase
import com.example.engine.AovLane
import com.example.engine.BotTacticalAction
import com.example.engine.MicroBotBrain
import com.example.engine.TacticalInputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun ruleFsm_recallsWhenSafeAndLowHp_andRetreatsWhenEnemyClose() {
        val profile = BotProfileEntity(
            name = "Florentino Top",
            selectedHeroId = "FLORENTINO",
            assignedLane = "TOP_CAESAR",
            heroRole = HeroRole.WARRIOR.name,
            brainMode = BrainMode.HYBRID.name,
            retreatHpPercent = 25,
            autoDodgeSkillshot = true
        )
        // Khi máu thấp (18%) nhưng địch ở xa (dist = 0.65) -> Bấm Biến Về (SAFE_RECALL_BASE)
        val lowHpSafeState = TacticalInputState(
            botHpRatio = 0.18f,
            botManaRatio = 0.5f,
            enemyHpRatio = 0.8f,
            distToEnemyNorm = 0.65f,
            distToAllyTowerNorm = 0.3f,
            enemyUnderTower = 0f,
            skillsReadyRatio = 0.6f,
            incomingProjectileThreat = 0f
        )
        val resRecall = MicroBotBrain.decide(lowHpSafeState, profile)
        assertEquals(BotTacticalAction.SAFE_RECALL_BASE, resRecall.action)

        // Khi máu thấp (18%) và địch áp sát (dist = 0.20) -> Bấm Cấp cứu / Tốc biến chạy về trụ (RETREAT_HEAL)
        val lowHpCloseEnemy = lowHpSafeState.copy(distToEnemyNorm = 0.20f)
        val resRetreat = MicroBotBrain.decide(lowHpCloseEnemy, profile)
        assertEquals(BotTacticalAction.RETREAT_HEAL, resRetreat.action)

        // Khi có đạn định hướng bay tới -> Lách 90 độ
        val dodgeState = lowHpSafeState.copy(incomingProjectileThreat = 1.0f)
        val resDodge = MicroBotBrain.decide(dodgeState, profile)
        assertEquals(BotTacticalAction.DODGE_SKILLSHOT, resDodge.action)
    }

    @Test
    fun heroMetaDatabase_andMicroMlp_workCorrectly() {
        val draft = AovHeroAndMetaDatabase.analyzeDraftAndMeta(
            AovLane.TOP_CAESAR,
            setOf("HEAL_TANK", "HARD_CC")
        )
        assertTrue(draft.priorityBans.isNotEmpty())
        assertTrue(draft.counterBuildAdvice.any { it.contains("Truy Hồn") })

        val initWeights = MicroBotBrain.createPresetWeights(HeroRole.ASSASSIN)
        val report = MicroBotBrain.trainOnDevice(
            initialWeights = initWeights,
            role = HeroRole.ASSASSIN,
            retreatHpPercent = 25,
            epochs = 200
        )
        assertEquals(MicroBotBrain.TOTAL_WEIGHTS, report.updatedWeights.size)
        assertTrue(report.accuracyPercent >= 70f)
    }
}
