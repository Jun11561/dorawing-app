package com.team3.dorawing.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class HealthCardsTest {

    private val day = LocalDate.of(2026, 10, 29)

    @Test
    fun `카드가 많으면 하루에 3장을 겹치지 않게 고른다`() {
        val picked = HealthCards.pickToday(10, day)
        assertEquals(3, picked.size)
        assertEquals(3, picked.toSet().size)
        assertTrue(picked.all { it in 0 until 10 })
    }

    @Test
    fun `카드가 3장보다 적으면 있는 만큼만 보여 준다`() {
        assertEquals(listOf(0), HealthCards.pickToday(1, day))
        assertEquals(2, HealthCards.pickToday(2, day).toSet().size)
        assertEquals(emptyList<Int>(), HealthCards.pickToday(0, day))
    }

    @Test
    fun `같은 날은 같은 카드 다음 날은 다른 카드`() {
        assertEquals(HealthCards.pickToday(10, day), HealthCards.pickToday(10, day))
        assertNotEquals(HealthCards.pickToday(10, day), HealthCards.pickToday(10, day.plusDays(1)))
    }

    @Test
    fun `카드 파일의 아이콘 이름은 모두 그림이 있다`() {
        val text = File("src/main/res/raw/health_cards.json").readText()
        val icons = Regex("\"icon\"\\s*:\\s*\"([^\"]*)\"").findAll(text).map { it.groupValues[1] }.toList()
        val cards = Regex("\"title\"").findAll(text).count()
        assertEquals(cards, icons.size)
        icons.forEach { assertNotNull(it, HealthCards.iconFor(it)) }
    }

    @Test
    fun `유튜브 링크만 영상 안내를 쓴다`() {
        assertTrue(HealthCards.isVideo("https://www.youtube.com/watch?v=abc"))
        assertTrue(HealthCards.isVideo("https://youtu.be/abc"))
        assertFalse(HealthCards.isVideo("https://www.nid.or.kr/notification/prevention.aspx"))
    }

    @Test
    fun `전화 버튼은 콜센터 카드에만 있다`() {
        val text = File("src/main/res/raw/health_cards.json").readText()
        val phones = Regex("\"phone\"\\s*:\\s*\"([^\"]*)\"").findAll(text).map { it.groupValues[1] }.toList()
        assertEquals(listOf("18999988"), phones)
    }
}
