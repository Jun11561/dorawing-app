package com.team3.dorawing.game

import android.content.Context
import androidx.room.withTransaction
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.db.Attendance
import com.team3.dorawing.db.GameSetResult
import com.team3.dorawing.db.LevelState
import com.team3.dorawing.util.Dates
import java.io.Serializable

data class FinishSummary(
    val correct: Int,
    val total: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val stars: Int
) : Serializable

// 게임 화면은 세트가 끝나면 finish를 한 번만 부른다
object GameRecorder {

    // playedAt은 디버그 메뉴에서 과거 날짜 기록을 넣을 때만 바꾼다
    suspend fun finish(
        context: Context,
        gameType: GameType,
        level: Int,
        correct: Int,
        total: Int,
        avgReactionMs: Long,
        playedAt: Long = System.currentTimeMillis()
    ): FinishSummary {
        val db = AppDatabase.get(context)
        val type = gameType.name

        return db.withTransaction {
            // 1. 세트 기록 저장
            db.gameSetDao().insert(
                GameSetResult(
                    gameType = type,
                    level = level,
                    correct = correct,
                    total = total,
                    avgReactionMs = avgReactionMs,
                    playedAt = playedAt
                )
            )

            // 2. 레벨 갱신
            val state = db.levelStateDao().get(type)
                ?: LevelState(type, Difficulty.startLevel(db.diagnosisDao().last()), 0, 0)
            val rates = db.gameSetDao().recent(type, 3).reversed().map { it.rate }
            val newState = Difficulty.nextLevel(state, rates)
            db.levelStateDao().upsert(newState)

            // 3. 출석
            val day = Dates.dayText(playedAt)
            val sets = db.attendanceDao().get(day)?.completedSets ?: 0
            db.attendanceDao().upsert(Attendance(day, sets + 1))

            // 4. 변화 감지
            val alert = ChangeCheck.check(
                db.gameSetDao().allOf(type),
                Dates.toDate(playedAt),
                db.changeAlertDao().allOf(type)
            )
            if (alert != null) db.changeAlertDao().insert(alert)

            FinishSummary(correct, total, state.level, newState.level, stars(correct, total))
        }
    }

    // 90% 이상 별 3개, 70% 이상 2개, 그 외 1개
    fun stars(correct: Int, total: Int): Int {
        val rate = if (total > 0) correct.toFloat() / total else 0f
        return when {
            rate >= 0.9f -> 3
            rate >= 0.7f -> 2
            else -> 1
        }
    }
}
