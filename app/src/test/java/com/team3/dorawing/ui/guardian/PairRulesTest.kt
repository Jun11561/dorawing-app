package com.team3.dorawing.ui.guardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PairRulesTest {

    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    @Test
    fun `코드는 숫자 6자리`() {
        val random = Random(7)
        repeat(200) {
            val code = PairRules.newCode(random)
            assertTrue(code, PairRules.isValidFormat(code))
        }
        assertTrue(PairRules.isValidFormat(PairRules.newCode()))
        assertFalse(PairRules.isValidFormat("12345"))
        assertFalse(PairRules.isValidFormat("1234567"))
        assertFalse(PairRules.isValidFormat("12a456"))
        assertTrue(PairRules.isValidFormat("012345"))
    }

    @Test
    fun `코드는 10분 동안만 쓸 수 있다`() {
        val expires = PairRules.expiresAt(now)
        assertEquals(now + 10 * minute, expires)
        assertTrue(PairRules.isUsable(expires, used = false, now = now))
        assertTrue(PairRules.isUsable(expires, used = false, now = now + 10 * minute - 1))
        assertFalse(PairRules.isUsable(expires, used = false, now = now + 10 * minute))
    }

    @Test
    fun `한 번 쓴 코드는 다시 못 쓴다`() {
        val expires = PairRules.expiresAt(now)
        assertFalse(PairRules.isUsable(expires, used = true, now = now + minute))
    }

    @Test
    fun `보호자는 2명까지`() {
        assertTrue(PairRules.canAddGuardian(0))
        assertTrue(PairRules.canAddGuardian(1))
        assertFalse(PairRules.canAddGuardian(2))
        assertFalse(PairRules.canAddGuardian(3))
    }

    @Test
    fun `4번 틀리면 1번 남고 5번째에 10분 동안 막는다`() {
        var lock = PairRules.Lock(0, 0)
        repeat(4) { lock = PairRules.afterFail(lock, now) }
        assertFalse(PairRules.isLocked(lock, now))
        assertEquals(1, PairRules.triesLeft(lock))

        lock = PairRules.afterFail(lock, now)
        assertTrue(PairRules.isLocked(lock, now))
        assertTrue(PairRules.isLocked(lock, now + 10 * minute - 1))
        assertFalse(PairRules.isLocked(lock, now + 10 * minute))
        assertEquals(10, PairRules.minutesLeft(lock, now))
        assertEquals(1, PairRules.minutesLeft(lock, now + 9 * minute + 30_000))
    }

    @Test
    fun `잠금이 풀린 뒤에는 다시 5번 넣어 볼 수 있다`() {
        var lock = PairRules.Lock(0, 0)
        repeat(5) { lock = PairRules.afterFail(lock, now) }
        val later = now + 11 * minute
        assertFalse(PairRules.isLocked(lock, later))
        assertEquals(5, PairRules.triesLeft(lock))

        lock = PairRules.afterFail(lock, later)
        assertEquals(4, PairRules.triesLeft(lock))
        assertFalse(PairRules.isLocked(lock, later))
    }
}
