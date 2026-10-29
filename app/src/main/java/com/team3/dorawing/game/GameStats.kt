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

    // 최근 3세트 평균 정답률 (0~1), 기록이 없으면 null
    suspend fun recentAverage(context: Context, type: GameType): Float? {
        val sets = AppDatabase.get(context).gameSetDao().recent(type.name, 3)
        if (sets.isEmpty()) return null
        return sets.map { it.rate }.average().toFloat()
    }
}
