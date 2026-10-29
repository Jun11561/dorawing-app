package com.team3.dorawing.ui.home

import java.time.LocalDate

data class HealthCard(
    val type: String,
    val title: String,
    val body: String,
    val link: String
)

object HealthCards {
    const val PER_DAY = 3

    // 오늘 보여 줄 카드 번호. 날마다 다음 묶음으로 넘어가고 끝까지 가면 처음으로 돌아간다
    fun pickToday(cardCount: Int, today: LocalDate): List<Int> {
        if (cardCount <= 0) return emptyList()
        val count = minOf(PER_DAY, cardCount)
        val start = Math.floorMod(today.toEpochDay() * count, cardCount.toLong()).toInt()
        return (0 until count).map { (start + it) % cardCount }
    }
}
