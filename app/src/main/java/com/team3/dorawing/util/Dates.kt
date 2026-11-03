package com.team3.dorawing.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val koreanFormat = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    private val timeFormat = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)

    // 화면에 보여 줄 날짜. 예: 2026년 10월 29일
    fun koreanDay(millis: Long): String = toDate(millis).format(koreanFormat)

    // 날짜와 시각. 예: 11월 3일 오후 4:20
    fun koreanTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

    fun toDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    // 그날 0시
    fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun dayText(date: LocalDate): String = date.format(dayFormat)

    fun dayText(millis: Long): String = dayText(toDate(millis))

    // 이번 주 월요일
    fun monday(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
}
