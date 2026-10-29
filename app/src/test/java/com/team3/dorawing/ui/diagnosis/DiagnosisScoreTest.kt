package com.team3.dorawing.ui.diagnosis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisScoreTest {

    @Test
    fun `모두 아니다면 0점이고 정상이다`() {
        val total = DiagnosisScore.total(List(15) { 0 })
        assertEquals(0, total)
        assertFalse(DiagnosisScore.isSuspected(total))
    }

    @Test
    fun `5점은 정상이다`() {
        val total = DiagnosisScore.total(List(5) { 1 } + List(10) { 0 })
        assertEquals(5, total)
        assertFalse(DiagnosisScore.isSuspected(total))
    }

    @Test
    fun `6점부터 인지 저하 의심이다`() {
        val total = DiagnosisScore.total(List(6) { 1 } + List(9) { 0 })
        assertEquals(6, total)
        assertTrue(DiagnosisScore.isSuspected(total))
    }

    @Test
    fun `모두 자주 그렇다면 30점이다`() {
        val total = DiagnosisScore.total(List(15) { 2 })
        assertEquals(30, total)
        assertTrue(DiagnosisScore.isSuspected(total))
    }

    @Test
    fun `중간에 2점 문항이 섞여도 모두 더한다`() {
        // 2 + 1 + 2 = 5점, 나머지는 0
        val answers = listOf(0, 2, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(5, DiagnosisScore.total(answers))
        assertFalse(DiagnosisScore.isSuspected(5))

        // 2점 세 개면 6점
        val more = listOf(2, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0)
        assertEquals(6, DiagnosisScore.total(more))
        assertTrue(DiagnosisScore.isSuspected(6))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `0 1 2 밖의 답은 받지 않는다`() {
        DiagnosisScore.total(listOf(0, 3, 1))
    }
}
