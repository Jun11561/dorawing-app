package com.team3.dorawing.game

import android.content.Context
import com.team3.dorawing.db.AppDatabase

// 게임 선택 화면이 쓰는 조회 함수
object GameStats {

    // 기록이 없으면 시작 레벨 규칙을 따른다
    suspend fun currentLevel(context: Context, type: GameType): Int {
        val db = AppDatabase.get(context)
        return db.levelStateDao().get(type.name)?.level
            ?: Difficulty.startLevel(db.diagnosisDao().last())
    }

    // 가장 낮은 영역이 다른 영역 평균보다 10%p 이상 낮을 때만 그 영역, 아니면 null
    // 가장 낮은 영역이 둘 이상이면 하나만 칠할 수 없어서 null
    fun lowestArea(rates: List<Pair<GameType, Double>>): GameType? {
        if (rates.size < 2) return null
        val low = rates.minOf { it.second }
        val lows = rates.filter { it.second == low }
        if (lows.size > 1) return null
        val others = rates.filter { it.second != low }.map { it.second }.average()
        // 0.1을 소수로 빼면 조금 모자랄 수 있어서 %p로 반올림해 비교한다
        return if (Math.round((others - low) * 1000) >= 100) lows[0].first else null
    }

    // 최근 3세트 평균 정답률 (0~1), 기록이 없으면 null
    suspend fun recentAverage(context: Context, type: GameType): Float? {
        val sets = AppDatabase.get(context).gameSetDao().recent(type.name, 3)
        if (sets.isEmpty()) return null
        return sets.map { it.rate }.average().toFloat()
    }
}
