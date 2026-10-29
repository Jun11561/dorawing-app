package com.team3.dorawing.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// gameType은 GameType 이름을 문자열로 저장한다.
// 게임 목록이 바뀌어 모르는 이름이 남아 있어도 앱이 꺼지지 않게 하려고 enum으로 바꾸지 않는다.

@Entity
data class DiagnosisResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val takenAt: Long,
    val totalScore: Int,
    val isSuspected: Boolean
)

@Entity
data class GameSetResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameType: String,
    val level: Int,
    val correct: Int,
    val total: Int,
    val avgReactionMs: Long,
    val playedAt: Long
) {
    val rate: Float get() = if (total > 0) correct.toFloat() / total else 0f
}

@Entity
data class Attendance(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val completedSets: Int
)

@Entity
data class LevelState(
    @PrimaryKey val gameType: String,
    val level: Int,
    val setsSinceLevelChange: Int,
    val totalSets: Int
)

@Entity
data class ChangeAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameType: String,
    val recentAvg: Float,
    val baselineAvg: Float,
    val createdAt: Long,
    val dismissed: Boolean = false
)
