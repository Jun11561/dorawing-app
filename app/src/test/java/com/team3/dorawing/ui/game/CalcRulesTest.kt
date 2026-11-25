package com.team3.dorawing.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CalcRulesTest {

    @Test
    fun `1단계는 천 원 단위 물건 두 개의 덧셈`() {
        repeat(10) {
            val p = CalcRules.makeSum(1, Random(it))
            assertEquals(2, p.items.size)
            assertTrue(p.items.all { (item, count) -> item.price % 1000 == 0 && count == 1 })
            assertEquals(p.items.sumOf { it.first.price }, p.answer)
        }
    }

    @Test
    fun `3단계 거스름돈은 낸 돈에서 합계를 뺀 값이고 0보다 크다`() {
        repeat(10) {
            val p = CalcRules.makeSum(3, Random(it))
            val total = p.items.sumOf { it.first.price * it.second }
            assertEquals(p.pay!! - total, p.answer)
            assertTrue(p.answer > 0)
        }
    }

    @Test
    fun `4단계는 물건 하나를 여러 개 사는 곱셈`() {
        repeat(10) {
            val p = CalcRules.makeSum(4, Random(it))
            assertEquals(1, p.items.size)
            assertTrue(p.items[0].second >= 2)
            assertEquals(p.items[0].first.price * p.items[0].second, p.answer)
        }
    }

    @Test
    fun `보기 4개에 정답이 하나만 있고 모두 0보다 크다`() {
        for (level in 1..5) repeat(10) {
            val p = CalcRules.makeSum(level, Random(it))
            assertEquals(4, p.choices.size)
            assertEquals(4, p.choices.distinct().size)
            assertEquals(1, p.choices.count { c -> c == p.answer })
            assertTrue(p.choices.all { c -> c > 0 })
        }
    }

    @Test
    fun `내는 돈은 합계보다 크다`() {
        assertEquals(5000, CalcRules.payFor(3700))
        assertEquals(4000, CalcRules.payFor(2000))
    }

    @Test
    fun `돈 내기 금액은 단계별 단위를 지킨다`() {
        repeat(20) {
            assertEquals(0, CalcRules.makePay(1, Random(it)) % 1000)
            assertEquals(0, CalcRules.makePay(2, Random(it)) % 500)
            assertEquals(0, CalcRules.makePay(5, Random(it)) % 100)
        }
    }

    @Test
    fun `도착 시각은 12시를 넘지 않고 보기에 정답이 하나 있다`() {
        for (level in 1..5) repeat(20) {
            val p = CalcRules.makeTime(level, Random(it))
            assertEquals(p.start + p.takes, p.answer)
            assertTrue(p.answer <= 12 * 60)
            assertEquals(1, p.choices.count { c -> c == p.answer })
            assertEquals(p.choices.size, p.choices.distinct().size)
        }
    }

    @Test
    fun `시각과 시계 글자`() {
        assertEquals("2시", CalcRules.timeText(120))
        assertEquals("2시 30분", CalcRules.timeText(150))
        assertEquals("1시간 30분", CalcRules.takesText(90))
        assertEquals("30분", CalcRules.takesText(30))
        assertEquals("🕑", CalcRules.clockEmoji(120))  // 🕑 2시
        assertEquals("🕝", CalcRules.clockEmoji(150))  // 🕝 2시 30분
    }
}
