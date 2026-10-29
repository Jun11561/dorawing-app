package com.team3.dorawing.game

import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.GameSetResult
import com.team3.dorawing.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChangeCheckTest {

    private val today = LocalDate.of(2026, 10, 29)

    // daysAgo일 전에 정답 수 correct / 10 인 세트
    private fun set(daysAgo: Long, correct: Int, type: String = "ATTENTION") = GameSetResult(
        gameType = type,
        level = 3,
        correct = correct,
        total = 10,
        avgReactionMs = 1000,
        playedAt = Dates.toMillis(today.minusDays(daysAgo)) + 10 * 60 * 60 * 1000 // 오전 10시
    )

    // 오늘부터 7일 동안 매일 한 세트
    private fun recentWeek(correct: Int) = (0L..6L).map { set(it, correct) }

    // 7일 전부터 34일 전까지 매일 한 세트
    private fun before(days: Long, correct: Int) = (7L until 7L + days).map { set(it, correct) }

    @Test
    fun `이전 28일 평균 80퍼센트 최근 7일 평균 60퍼센트면 알림을 만든다`() {
        val alert = ChangeCheck.check(before(28, 8) + recentWeek(6), today, emptyList())
        assertNotNull(alert)
        assertEquals("ATTENTION", alert!!.gameType)
        assertEquals(0.6f, alert.recentAvg, 0.001f)
        assertEquals(0.8f, alert.baselineAvg, 0.001f)
    }

    @Test
    fun `차이가 14퍼센트포인트면 알림이 없다`() {
        // 이전 80퍼센트, 최근 7일은 7세트 중 66퍼센트 근처가 되도록 섞는다
        val recent = listOf(set(0, 7), set(1, 7), set(2, 6), set(3, 7), set(4, 6), set(5, 7), set(6, 6))
        // 최근 평균 = 46 / 70 = 0.657, 차이 0.143
        assertNull(ChangeCheck.check(before(28, 8) + recent, today, emptyList()))
    }

    @Test
    fun `최근 7일 세트가 2개면 판단하지 않는다`() {
        val results = before(28, 8) + listOf(set(0, 2), set(3, 2))
        assertNull(ChangeCheck.check(results, today, emptyList()))
    }

    @Test
    fun `기록이 3주뿐이면 최근 7일 이전 전체 평균과 비교한다`() {
        // 7일 전부터 20일 전까지 14세트만 있다
        val alert = ChangeCheck.check(before(14, 9) + recentWeek(6), today, emptyList())
        assertNotNull(alert)
        assertEquals(0.9f, alert!!.baselineAvg, 0.001f)
    }

    @Test
    fun `같은 영역 알림이 5일 전에 있으면 다시 만들지 않는다`() {
        val old = ChangeAlert(
            gameType = "ATTENTION",
            recentAvg = 0.5f,
            baselineAvg = 0.8f,
            createdAt = Dates.toMillis(today.minusDays(5))
        )
        assertNull(ChangeCheck.check(before(28, 8) + recentWeek(6), today, listOf(old)))

        // 7일이 지났으면 다시 만든다
        val older = old.copy(createdAt = Dates.toMillis(today.minusDays(7)))
        assertNotNull(ChangeCheck.check(before(28, 8) + recentWeek(6), today, listOf(older)))
    }

    @Test
    fun `비교 기준 세트가 2개면 판단하지 않는다`() {
        assertNull(ChangeCheck.check(before(2, 9) + recentWeek(3), today, emptyList()))
    }

    @Test
    fun `35일보다 오래된 기록은 비교 기준에 넣지 않는다`() {
        // 아주 오래전 높은 점수는 빼고, 직전 28일 평균 70퍼센트와 최근 60퍼센트를 비교하면 알림이 없다
        val old = (40L..50L).map { set(it, 10) }
        assertNull(ChangeCheck.check(old + before(28, 7) + recentWeek(6), today, emptyList()))
    }
}
