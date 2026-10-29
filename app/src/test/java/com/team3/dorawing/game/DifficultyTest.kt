package com.team3.dorawing.game

import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.db.LevelState
import org.junit.Assert.assertEquals
import org.junit.Test

class DifficultyTest {

    private fun state(level: Int, since: Int, total: Int) = LevelState("ATTENTION", level, since, total)

    @Test
    fun `보정 구간 첫 세트 90퍼센트면 바로 한 단계 올린다`() {
        val next = Difficulty.nextLevel(state(2, 0, 0), listOf(0.9f))
        assertEquals(3, next.level)
        assertEquals(0, next.setsSinceLevelChange)
        assertEquals(1, next.totalSets)
    }

    @Test
    fun `보정 구간 두 번째 세트 40퍼센트면 바로 한 단계 내린다`() {
        val next = Difficulty.nextLevel(state(3, 0, 1), listOf(0.9f, 0.4f))
        assertEquals(2, next.level)
        assertEquals(0, next.setsSinceLevelChange)
    }

    @Test
    fun `보정 구간 이후 레벨 변경 뒤 세트가 2개면 유지한다`() {
        val next = Difficulty.nextLevel(state(3, 1, 5), listOf(0.9f, 0.9f, 0.9f))
        assertEquals(3, next.level)
        assertEquals(2, next.setsSinceLevelChange)
    }

    @Test
    fun `레벨 변경 뒤 3세트 평균 80퍼센트면 올리고 세트 수를 0으로 되돌린다`() {
        val next = Difficulty.nextLevel(state(3, 2, 6), listOf(0.7f, 0.8f, 0.9f))
        assertEquals(4, next.level)
        assertEquals(0, next.setsSinceLevelChange)
        assertEquals(7, next.totalSets)
    }

    @Test
    fun `3세트 평균 49퍼센트면 내린다`() {
        val next = Difficulty.nextLevel(state(3, 2, 6), listOf(0.47f, 0.5f, 0.5f))
        assertEquals(2, next.level)
    }

    @Test
    fun `3세트 평균 50퍼센트와 79퍼센트는 유지한다`() {
        val half = Difficulty.nextLevel(state(3, 2, 6), listOf(0.5f, 0.5f, 0.5f))
        assertEquals(3, half.level)
        assertEquals(3, half.setsSinceLevelChange)

        val almost = Difficulty.nextLevel(state(3, 2, 6), listOf(0.79f, 0.79f, 0.79f))
        assertEquals(3, almost.level)
    }

    @Test
    fun `5레벨에서 더 오르지 않고 1레벨에서 더 내려가지 않는다`() {
        val top = Difficulty.nextLevel(state(5, 2, 6), listOf(1f, 1f, 1f))
        assertEquals(5, top.level)

        val bottom = Difficulty.nextLevel(state(1, 2, 6), listOf(0.1f, 0.1f, 0.1f))
        assertEquals(1, bottom.level)

        val firstTop = Difficulty.nextLevel(state(5, 0, 0), listOf(1f))
        assertEquals(5, firstTop.level)
    }

    @Test
    fun `4세트 이상 주면 최근 3세트만 평균한다`() {
        // 맨 앞 0퍼센트를 넣으면 평균이 80퍼센트 아래로 떨어지지만 최근 3세트만 보므로 올린다
        val next = Difficulty.nextLevel(state(3, 3, 8), listOf(0f, 0.8f, 0.8f, 0.8f))
        assertEquals(4, next.level)
    }

    @Test
    fun `시작 레벨은 정상 3 의심 1 안 했으면 2`() {
        assertEquals(2, Difficulty.startLevel(null))
        assertEquals(3, Difficulty.startLevel(DiagnosisResult(takenAt = 0, totalScore = 3, isSuspected = false)))
        assertEquals(1, Difficulty.startLevel(DiagnosisResult(takenAt = 0, totalScore = 8, isSuspected = true)))
    }
}
