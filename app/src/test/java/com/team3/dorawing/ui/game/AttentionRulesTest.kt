package com.team3.dorawing.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AttentionRulesTest {

    @Test
    fun `같은 색은 뜻과 색이 같고 정답은 그 색`() {
        repeat(20) {
            val p = AttentionRules.make(AttentionRules.SAME, 3, Random(it))
            assertEquals(p.word, p.ink)
            assertEquals(p.ink, p.answer)
        }
    }

    @Test
    fun `다른 색은 뜻과 색이 다르고 정답은 글자 색`() {
        repeat(20) {
            val p = AttentionRules.make(AttentionRules.DIFF, 3, Random(it))
            assertNotEquals(p.word, p.ink)
            assertEquals(p.ink, p.answer)
        }
    }

    @Test
    fun `규칙 바꾸기는 테두리가 있으면 뜻 없으면 색이 정답`() {
        repeat(30) {
            val p = AttentionRules.make(AttentionRules.SWITCH, 5, Random(it))
            assertNotEquals(p.word, p.ink)
            assertEquals(if (p.framed) p.word else p.ink, p.answer)
        }
    }

    @Test
    fun `색 번호는 레벨의 색 수를 넘지 않는다`() {
        for (level in 1..5) {
            val n = AttentionRules.colorCount(level)
            repeat(20) {
                val p = AttentionRules.make(AttentionRules.SWITCH, level, Random(it))
                assertTrue(p.word < n && p.ink < n)
            }
        }
    }

    @Test
    fun `레벨이 오르면 다른 색과 규칙 바꾸기가 줄지 않는다`() {
        var before = 0
        for (level in 1..5) {
            val now = AttentionRules.problems(level).count { it != AttentionRules.SAME }
            assertTrue("Lv.$level", now >= before)
            before = now
        }
    }
}
