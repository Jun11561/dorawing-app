package com.team3.dorawing.ui.guardian

import android.content.Context
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.db.Attendance
import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.ui.diagnosis.DiagnosisScore
import com.team3.dorawing.util.Dates
import java.time.LocalDate

// 보호자에게 올리는 요약. 여기 있는 칸만 서버에 올라간다.
// 자가진단 문항별 답, 이름, 연락처, 위치, 게임 세트별 기록은 칸 자체가 없다
data class Summary(
    val day: String,
    val updatedAt: Long,
    val diagnosis: Diagnosis?,
    val areas: Map<String, Area>,
    val attendDays: List<String>,
    val lastPlayedAt: Long?,
    val alerts: List<Alert>
) {
    data class Diagnosis(val score: Int, val maxScore: Int, val suspected: Boolean, val takenAt: Long)

    // rate는 최근 3세트 평균(0~1), 기록이 없으면 null
    data class Area(val rate: Double?, val level: Int)

    data class Alert(val area: String, val day: String)

    fun toMap(): Map<String, Any?> = mapOf(
        "day" to day,
        "updatedAt" to updatedAt,
        "diagnosis" to diagnosis?.let {
            mapOf("score" to it.score, "maxScore" to it.maxScore, "suspected" to it.suspected, "takenAt" to it.takenAt)
        },
        "areas" to areas.mapValues { mapOf("rate" to it.value.rate, "level" to it.value.level) },
        "attendDays" to attendDays,
        "lastPlayedAt" to lastPlayedAt,
        "alerts" to alerts.map { mapOf("area" to it.area, "day" to it.day) }
    )

    companion object {
        // firestore.rules의 validSummary와 같아야 한다 (SummaryTest가 확인)
        val KEYS = setOf("day", "updatedAt", "diagnosis", "areas", "attendDays", "lastPlayedAt", "alerts")

        // 출석은 지난 14일만 올리고 보호자 화면이 그중 이번 주를 고른다
        const val ATTEND_DAYS = 14L

        // 변화 알림은 최근 7일에 생긴 것만
        const val ALERT_DAYS = 7L

        fun build(
            today: LocalDate,
            now: Long,
            diagnosis: DiagnosisResult?,
            rates: Map<String, Float?>,
            levels: Map<String, Int>,
            attendance: List<Attendance>,
            lastPlayedAt: Long?,
            alerts: List<ChangeAlert>
        ): Summary {
            val attendFrom = Dates.dayText(today.minusDays(ATTEND_DAYS - 1))
            val alertFrom = today.minusDays(ALERT_DAYS - 1)
            return Summary(
                day = Dates.dayText(today),
                updatedAt = now,
                diagnosis = diagnosis?.let { Diagnosis(it.totalScore, DiagnosisScore.MAX_SCORE, it.isSuspected, it.takenAt) },
                areas = levels.mapValues { (type, level) -> Area(rates[type]?.toDouble(), level) },
                attendDays = attendance.filter { it.completedSets > 0 && it.date >= attendFrom }.map { it.date }.sorted(),
                lastPlayedAt = lastPlayedAt,
                alerts = alerts.filter { !Dates.toDate(it.createdAt).isBefore(alertFrom) }
                    .map { Alert(it.gameType, Dates.dayText(it.createdAt)) }
            )
        }

        // 휴대폰 DB에서 공유 항목만 모은다
        suspend fun collect(context: Context): Summary {
            val db = AppDatabase.get(context)
            val today = LocalDate.now()
            val rates = mutableMapOf<String, Float?>()
            val levels = mutableMapOf<String, Int>()
            for (type in GameType.entries) {
                rates[type.name] = GameStats.recentAverage(context, type)
                levels[type.name] = GameStats.currentLevel(context, type)
            }
            val attendance = db.attendanceDao().between(Dates.dayText(today.minusDays(ATTEND_DAYS - 1)), Dates.dayText(today))
            val alerts = GameType.entries.flatMap { db.changeAlertDao().allOf(it.name) }
            return build(
                today, System.currentTimeMillis(), db.diagnosisDao().last(), rates, levels,
                attendance, db.gameSetDao().last()?.playedAt, alerts
            )
        }

        // 서버에서 받은 값. 숫자는 Long이나 Double로 온다
        fun fromMap(map: Map<String, Any?>): Summary {
            val diag = map["diagnosis"] as? Map<*, *>
            val areas = (map["areas"] as? Map<*, *>).orEmpty().mapNotNull { (key, value) ->
                val area = value as? Map<*, *> ?: return@mapNotNull null
                key.toString() to Area((area["rate"] as? Number)?.toDouble(), (area["level"] as? Number)?.toInt() ?: 1)
            }.toMap()
            return Summary(
                day = map["day"] as? String ?: "",
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
                diagnosis = diag?.let {
                    Diagnosis(
                        (it["score"] as? Number)?.toInt() ?: 0,
                        (it["maxScore"] as? Number)?.toInt() ?: DiagnosisScore.MAX_SCORE,
                        it["suspected"] as? Boolean ?: false,
                        (it["takenAt"] as? Number)?.toLong() ?: 0L
                    )
                },
                areas = areas,
                attendDays = (map["attendDays"] as? List<*>).orEmpty().filterIsInstance<String>(),
                lastPlayedAt = (map["lastPlayedAt"] as? Number)?.toLong(),
                alerts = (map["alerts"] as? List<*>).orEmpty().mapNotNull { item ->
                    val alert = item as? Map<*, *> ?: return@mapNotNull null
                    Alert(alert["area"] as? String ?: "", alert["day"] as? String ?: "")
                }
            )
        }
    }
}
