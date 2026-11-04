package com.team3.dorawing.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStatsTest {

    @Test
    fun 다른_영역보다_10퍼센트포인트_낮으면_칠한다() {
        val rates = listOf(GameType.MEMORY to 0.6, GameType.CALC to 0.7, GameType.ATTENTION to 0.7)
        assertEquals(GameType.MEMORY, GameStats.lowestArea(rates))
    }

    @Test
    fun 차이가_10퍼센트포인트보다_작으면_칠하지_않는다() {
        val rates = listOf(GameType.MEMORY to 0.62, GameType.CALC to 0.7, GameType.ATTENTION to 0.7)
        assertNull(GameStats.lowestArea(rates))
    }

    @Test
    fun 다른_영역_평균과_비교한다() {
        // 가장 높은 영역과는 14%p 차이지만 나머지 평균(58%)과는 8%p
        val rates = listOf(GameType.MEMORY to 0.52, GameType.CALC to 0.5, GameType.ATTENTION to 0.64)
        assertNull(GameStats.lowestArea(rates))
    }

    @Test
    fun 가장_낮은_영역이_둘이면_칠하지_않는다() {
        val rates = listOf(GameType.MEMORY to 0.3, GameType.CALC to 0.3, GameType.ATTENTION to 0.9)
        assertNull(GameStats.lowestArea(rates))
    }

    @Test
    fun 영역이_하나뿐이면_칠하지_않는다() {
        assertNull(GameStats.lowestArea(listOf(GameType.MEMORY to 0.2)))
    }
}
