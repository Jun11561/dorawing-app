package com.team3.dorawing.ui.home

import com.team3.dorawing.R
import java.time.LocalDate

data class HealthCard(
    val type: String,
    val title: String,
    val body: String,
    val link: String,
    val icon: String
)

object HealthCards {
    const val PER_DAY = 3

    // health_cards.json의 icon 이름을 그림으로. 모르는 이름이면 null
    fun iconFor(name: String): Int? = when (name) {
        "directions_walk" -> R.drawable.ic_card_directions_walk
        "restaurant" -> R.drawable.ic_card_restaurant
        "menu_book" -> R.drawable.ic_card_menu_book
        "no_drinks" -> R.drawable.ic_card_no_drinks
        "smoke_free" -> R.drawable.ic_card_smoke_free
        "health_and_safety" -> R.drawable.ic_card_health_and_safety
        "monitor_heart" -> R.drawable.ic_card_monitor_heart
        "groups" -> R.drawable.ic_card_groups
        "local_hospital" -> R.drawable.ic_card_local_hospital
        "sports_gymnastics" -> R.drawable.ic_card_sports_gymnastics
        "call" -> R.drawable.ic_card_call
        else -> null
    }

    // 오늘 보여 줄 카드 번호. 날마다 다음 묶음으로 넘어가고 끝까지 가면 처음으로 돌아간다
    fun pickToday(cardCount: Int, today: LocalDate): List<Int> {
        if (cardCount <= 0) return emptyList()
        val count = minOf(PER_DAY, cardCount)
        val start = Math.floorMod(today.toEpochDay() * count, cardCount.toLong()).toInt()
        return (0 until count).map { (start + it) % cardCount }
    }
}
