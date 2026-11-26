package com.team3.dorawing.ui.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LanguageRulesTest {

    @Test
    fun `속담마다 틀린 보기가 4개 있고 정답과 겹치지 않는다`() {
        for (p in LanguageRules.PROVERBS) {
            assertEquals(p.answer, 4, p.wrong.size)
            assertFalse(p.answer, p.answer in p.wrong)
        }
    }

    @Test
    fun `빈칸 채우기 보기 수는 단계를 따르고 정답이 하나 있다`() {
        for (level in 1..5) repeat(10) {
            val p = LanguageRules.makeBlank(level, Random(it))
            assertEquals(LanguageRules.choiceCount(level), p.choices.size)
            assertEquals(p.choices.size, p.choices.distinct().size)
            assertTrue(p.prompt.contains("[    ]"))
            assertFalse("빈칸 뒤 조사 앞에 공백", p.prompt.contains("]  "))
            assertTrue(p.answer in p.choices.indices)
        }
    }

    @Test
    fun `속담 전체 문장에는 빈칸이 없다`() {
        val p = LanguageRules.makeBlank(1, Random(1))
        val full = LanguageRules.fullProverb(p.prompt, p.choices[p.answer])
        assertFalse(full.contains("["))
    }

    @Test
    fun `이름 고르기는 낮은 단계에서 익숙한 물건만 낸다`() {
        repeat(20) {
            val p = LanguageRules.makeName(2, Random(it))
            assertTrue(LanguageRules.EASY_THINGS.any { t -> t.first == p.prompt })
            assertEquals(p.choices.size, p.choices.distinct().size)
        }
    }

    @Test
    fun `같은 종류 고르기의 정답은 그 종류에 없는 낱말이고 나머지는 모두 그 종류`() {
        for (level in 1..5) repeat(20) {
            val p = LanguageRules.makeGroup(level, Random(it))
            val members = LanguageRules.GROUPS.getValue(p.group)
            assertFalse(p.choices[p.answer] in members)
            p.choices.forEachIndexed { i, word -> if (i != p.answer) assertTrue(word in members) }
        }
    }

    @Test
    fun `받침에 따라 조사가 붙는다`() {
        assertEquals("사과는", LanguageRules.josa("사과", "은", "는"))
        assertEquals("책은", LanguageRules.josa("책", "은", "는"))
        assertEquals("\"오는\"이에요", LanguageRules.josa("\"오는\"", "이에요", "예요"))
        assertEquals("토끼예요", LanguageRules.josa("토끼", "이에요", "예요"))
        assertEquals("3시예요", LanguageRules.josa("3시", "이에요", "예요"))
        assertEquals("곱하기 2를", LanguageRules.josa("곱하기 2", "을", "를"))
        assertEquals("곱하기 3을", LanguageRules.josa("곱하기 3", "을", "를"))
    }

    @Test
    fun `종류 낱말은 서로 겹치지 않는다`() {
        val all = LanguageRules.GROUPS.values.flatten()
        assertEquals(all.size, all.distinct().size)
    }
}
