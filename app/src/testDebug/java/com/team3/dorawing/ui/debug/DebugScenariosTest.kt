package com.team3.dorawing.ui.debug

import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.GameSetResult
import com.team3.dorawing.game.ChangeCheck
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// 디버그 시나리오를 GameRecorder.finish와 같은 순서로 넣었을 때 어떤 알림이 생기는지 DB 없이 확인한다
class DebugScenariosTest {

    private val today = LocalDate.of(2026, 10, 29)

    // GameRecorder.finish처럼 세트를 하나씩 넣으면서 그 영역의 변화 감지를 돌린다
    private fun replay(sets: List<PlannedSet>): List<ChangeAlert> {
        val results = mutableListOf<GameSetResult>()
        val alerts = mutableListOf<ChangeAlert>()
        for (set in sets) {
            val day = today.minusDays(set.daysAgo.toLong())
            results += GameSetResult(
                gameType = set.type.name,
                level = 3,
                correct = set.correct,
                total = DebugScenarios.TOTAL,
                avgReactionMs = 1500,
                playedAt = Dates.toMillis(day) + (10 * 60 + set.order * 10) * 60_000L
            )
            val alert = ChangeCheck.check(
                results.filter { it.gameType == set.type.name },
                day,
                alerts.filter { it.gameType == set.type.name }
            )
            if (alert != null) alerts += alert
        }
        return alerts
    }

    @Test
    fun `시나리오 1은 모든 영역에 매일 1세트씩 35일이고 알림이 없다`() {
        val sets = DebugScenarios.steady()
        assertEquals(35 * GameType.entries.size, sets.size)
        assertTrue(sets.all { it.correct in 12..16 })
        assertEquals(emptyList<ChangeAlert>(), replay(sets))
    }

    @Test
    fun `시나리오 2는 첫 영역에만 알림이 하나 생긴다`() {
        val alerts = replay(DebugScenarios.oneDrop())
        assertEquals(1, alerts.size)
        assertEquals(GameType.entries.first().name, alerts[0].gameType)
    }

    @Test
    fun `시나리오 2에서 떨어진 것은 첫 영역의 최근 7일뿐이다`() {
        val dropType = GameType.entries.first()
        for (set in DebugScenarios.oneDrop()) {
            if (set.type == dropType && set.daysAgo < 7) assertTrue(set.correct in 6..9) // 30~45%
            else assertTrue(set.correct in 12..16) // 60~80%
        }
    }

    @Test
    fun `시나리오는 누를 때마다 같은 기록을 만든다`() {
        assertEquals(DebugScenarios.oneDrop(), DebugScenarios.oneDrop())
        assertEquals(DebugScenarios.steady(), DebugScenarios.steady())
    }

    @Test
    fun `시나리오 3은 오늘 앞의 두 영역 90퍼센트`() {
        val sets = DebugScenarios.todayTwo()
        assertEquals(GameType.entries.take(2), sets.map { it.type })
        assertTrue(sets.all { it.daysAgo == 0 && it.correct == 18 })
    }
}
