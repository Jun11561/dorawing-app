package com.team3.dorawing.game

import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.GameSetResult
import com.team3.dorawing.util.Dates
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 진단이 아니라 기록이 평소보다 떨어졌는지 알려 주는 기능
object ChangeCheck {
    private const val RECENT_DAYS = 7L
    private const val BASE_DAYS = 28L
    private const val MIN_SETS = 3
    private const val DROP = 0.15f
    private const val ALERT_GAP_DAYS = 7L
    private const val EPS = 0.0001f

    // results: 한 영역의 기록, recentAlerts: 그 영역에서 만든 알림
    fun check(results: List<GameSetResult>, today: LocalDate, recentAlerts: List<ChangeAlert>): ChangeAlert? {
        if (results.isEmpty()) return null
        val type = results.first().gameType

        // 최근 7일: 오늘 포함 7일. 비교 기준: 그 전 28일
        // 기록이 28일보다 짧으면 이 구간에 최근 7일 이전 기록이 전부 들어가므로 전체 평균과 비교하는 셈이 된다
        val recentStart = today.minusDays(RECENT_DAYS - 1)
        val baseStart = recentStart.minusDays(BASE_DAYS)

        val recent = results.filter {
            val day = Dates.toDate(it.playedAt)
            !day.isBefore(recentStart) && !day.isAfter(today)
        }
        val base = results.filter {
            val day = Dates.toDate(it.playedAt)
            !day.isBefore(baseStart) && day.isBefore(recentStart)
        }
        if (recent.size < MIN_SETS || base.size < MIN_SETS) return null

        val recentAvg = recent.map { it.rate }.average().toFloat()
        val baseAvg = base.map { it.rate }.average().toFloat()
        if (baseAvg - recentAvg < DROP - EPS) return null

        // 같은 영역 알림이 7일 안에 있으면 다시 만들지 않는다
        val hasRecentAlert = recentAlerts.any {
            it.gameType == type && ChronoUnit.DAYS.between(Dates.toDate(it.createdAt), today) < ALERT_GAP_DAYS
        }
        if (hasRecentAlert) return null

        return ChangeAlert(
            gameType = type,
            recentAvg = recentAvg,
            baselineAvg = baseAvg,
            createdAt = Dates.toMillis(today)
        )
    }
}
