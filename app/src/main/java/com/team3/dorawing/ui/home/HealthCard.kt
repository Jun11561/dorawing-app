package com.team3.dorawing.ui.home

import com.team3.dorawing.R
import java.time.LocalDate

data class HealthCard(
    val type: String,
    val title: String,
    val body: String,
    val link: String,
    val icon: String,
    // 전화 버튼을 보여 줄 번호. 콜센터 카드만 있고 나머지는 빈 문자열
    val phone: String = ""
)

object HealthCards {
    const val PER_DAY = 3

    // 유튜브 링크는 유튜브 앱이 열려서 안내 문구를 다르게 한다
    fun isVideo(link: String): Boolean = link.contains("youtube.com") || link.contains("youtu.be")

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
