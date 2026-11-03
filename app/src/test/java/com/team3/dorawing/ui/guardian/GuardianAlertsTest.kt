package com.team3.dorawing.ui.guardian

import com.team3.dorawing.game.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianAlertsTest {

    private val first = GameType.entries.first().name
    private val last = GameType.entries.last().name

    @Test
    fun `처음 보는 알림만 새 알림이다`() {
        val alerts = listOf(Summary.Alert(first, "2026-11-02"), Summary.Alert(last, "2026-11-03"))
        val shown = setOf("$first|2026-11-02")
        assertEquals(listOf(Summary.Alert(last, "2026-11-03")), GuardianAlerts.newAlerts(alerts, shown))
    }

    @Test
    fun `두 번 확인해도 한 번만 띄운다`() {
        val alerts = listOf(Summary.Alert(first, "2026-11-03"))
        val firstTime = GuardianAlerts.newAlerts(alerts, emptySet())
        assertEquals(1, firstTime.size)
        val shown = GuardianAlerts.shownAfter(alerts, emptySet(), firstTime)
        assertTrue(GuardianAlerts.newAlerts(alerts, shown).isEmpty())
    }

    @Test
    fun `같은 영역이라도 다른 날 생긴 알림은 새 알림이다`() {
        val shown = setOf("$first|2026-10-20")
        val alerts = listOf(Summary.Alert(first, "2026-11-03"))
        assertEquals(alerts, GuardianAlerts.newAlerts(alerts, shown))
    }

    @Test
    fun `지금 게임 목록에 없는 영역은 띄우지 않는다`() {
        val alerts = listOf(Summary.Alert("OLD_GAME", "2026-11-03"))
        assertTrue(GuardianAlerts.newAlerts(alerts, emptySet()).isEmpty())
    }

    @Test
    fun `요약에서 빠진 알림은 기억에서도 지운다`() {
        val shown = setOf("$first|2026-10-20", "$last|2026-11-01")
        val alerts = listOf(Summary.Alert(last, "2026-11-01"))
        assertEquals(setOf("$last|2026-11-01"), GuardianAlerts.shownAfter(alerts, shown, emptyList()))
    }
}
