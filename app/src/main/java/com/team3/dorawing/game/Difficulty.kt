package com.team3.dorawing.game

import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.db.LevelState

object Difficulty {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 5

    // 소수 계산 오차 때문에 80%가 79.99%로 나오는 걸 막는다
    private const val EPS = 0.0001f

    // 자가진단 정상 Lv.3, 의심 Lv.1, 안 했으면 Lv.2
    fun startLevel(diagnosis: DiagnosisResult?): Int = when {
        diagnosis == null -> 2
        diagnosis.isSuspected -> 1
        else -> 3
    }

    // state: 이번 세트 전의 상태, recentRates: 이번 세트를 포함한 최근 정답률 (오래된 것부터)
    fun nextLevel(state: LevelState, recentRates: List<Float>): LevelState {
        val totalSets = state.totalSets + 1
        val sinceChange = state.setsSinceLevelChange + 1

        val rate: Float? = when {
            recentRates.isEmpty() -> null
            // 처음 3세트는 그 세트 정답률만 보고 바로 조정
            totalSets <= 3 -> recentRates.last()
            // 그 뒤에는 레벨이 바뀐 뒤 3세트가 쌓여야 최근 3세트 평균으로 판단
            sinceChange >= 3 -> recentRates.takeLast(3).average().toFloat()
            else -> null
        }

        var level = state.level
        if (rate != null) {
            if (rate >= 0.8f - EPS) level++
            else if (rate < 0.5f - EPS) level--
        }
        level = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

        return state.copy(
            level = level,
            totalSets = totalSets,
            setsSinceLevelChange = if (level != state.level) 0 else sinceChange
        )
    }
}
