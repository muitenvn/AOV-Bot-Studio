package com.example

import com.example.data.BotProfileEntity
import com.example.data.BrainMode
import com.example.data.HeroRole
import com.example.engine.BotTacticalAction
import com.example.engine.MicroBotBrain
import com.example.engine.TacticalInputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun ruleFsm_retreatsOnLowHp_andDodgesProjectile() {
        val profile = BotProfileEntity(
            name = "Test Marksman",
            heroRole = HeroRole.MARKSMAN.name,
            brainMode = BrainMode.RULE_FSM.name,
            retreatHpPercent = 28,
            autoDodgeSkillshot = true
        )
        val lowHpState = TacticalInputState(
            botHpRatio = 0.18f,
            botManaRatio = 0.5f,
            enemyHpRatio = 0.8f,
            distToEnemyNorm = 0.4f,
            distToAllyTowerNorm = 0.5f,
            enemyUnderTower = 0f,
            skillsReadyRatio = 0.6f,
            incomingProjectileThreat = 0f
        )
        val res1 = MicroBotBrain.decide(lowHpState, profile)
        assertEquals(BotTacticalAction.RETREAT_HEAL, res1.action)

        val dodgeState = lowHpState.copy(incomingProjectileThreat = 1.0f)
        val res2 = MicroBotBrain.decide(dodgeState, profile)
        assertEquals(BotTacticalAction.DODGE_SKILLSHOT, res2.action)
    }

    @Test
    fun microMlp_trainsOnDeviceFastAndAccurate() {
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
