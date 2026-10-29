package com.team3.dorawing.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
