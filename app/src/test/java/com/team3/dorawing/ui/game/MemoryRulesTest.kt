package com.team3.dorawing.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRulesTest {

    @Test
    fun `세트에는 레벨마다 세 유형이 모두 들어 있다`() {
        for (level in 1..5) {
            val set = MemoryRules.problems(level)
            assertTrue("Lv.$level", MemoryRules.PAIRS in set)
            assertTrue("Lv.$level", MemoryRules.ORDER in set)
            assertTrue("Lv.$level", MemoryRules.RECALL in set)
        }
    }

    @Test
    fun `레벨이 오르면 순서와 고르기 유형 수가 줄지 않는다`() {
        var before = 0
        for (level in 1..5) {
            val now = MemoryRules.problems(level).count { it != MemoryRules.PAIRS }
            assertTrue("Lv.$level", now >= before)
            before = now
        }
    }

    @Test
    fun `카드 판은 짝수 장수이고 가로는 4칸까지`() {
        for (level in 1..5) {
            val (rows, cols) = MemoryRules.pairBoard(level)
            assertEquals("Lv.$level", 0, rows * cols % 2)
            assertTrue("Lv.$level", cols <= 4)
        }
    }

    @Test
    fun `카드 판 크기는 계획대로 4 6 12 16 20장`() {
        val sizes = (1..5).map { val (r, c) = MemoryRules.pairBoard(it); r * c }
        assertEquals(listOf(4, 6, 12, 16, 20), sizes)
    }

    @Test
    fun `순서 길이는 판의 칸 수보다 작다`() {
        for (level in 1..5) {
            val (rows, cols) = MemoryRules.orderBoard(level)
            assertTrue("Lv.$level", MemoryRules.orderLength(level) < rows * cols)
        }
    }

    @Test
    fun `보여 주는 그림은 보기 수보다 적지 않고 보여 주는 시간은 3초 이상`() {
        for (level in 1..5) {
            assertTrue("Lv.$level", MemoryRules.recallShown(level) >= 3)
            assertTrue("Lv.$level", MemoryRules.recallChoices(level) >= 4)
            assertTrue("Lv.$level", MemoryRules.recallShowMs(level) >= 3000L)
        }
    }
}
