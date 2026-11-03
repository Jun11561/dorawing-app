package com.team3.dorawing.ui.guardian

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

// 보호자 연결 번호(코드) 규칙. 서버 규칙(firestore.rules)과 같은 숫자를 쓴다
object PairRules {
    const val CODE_LENGTH = 6
    const val CODE_MINUTES = 10
    const val MAX_GUARDIANS = 2
    const val MAX_FAILS = 5
    const val LOCK_MINUTES = 10

    private const val MINUTE = 60_000L

    // 보호자 폰에서 코드를 틀린 횟수와 입력을 막아 둔 시각
    data class Lock(val fails: Int, val lockedUntil: Long)

    // 짐작하기 어렵게 SecureRandom으로 6자리
    fun newCode(random: Random = SecureRandom().asKotlinRandom()): String =
        (1..CODE_LENGTH).joinToString("") { random.nextInt(10).toString() }

    fun isValidFormat(code: String): Boolean = code.length == CODE_LENGTH && code.all { it in '0'..'9' }

    fun expiresAt(now: Long): Long = now + CODE_MINUTES * MINUTE

    // 1회용: 쓰였거나 10분이 지나면 못 쓴다
    fun isUsable(expiresAt: Long, used: Boolean, now: Long): Boolean = !used && now < expiresAt

    fun canAddGuardian(count: Int): Boolean = count < MAX_GUARDIANS

    fun isLocked(lock: Lock, now: Long): Boolean = now < lock.lockedUntil

    // 5번째로 틀리면 10분 동안 막고 횟수는 처음부터
    fun afterFail(lock: Lock, now: Long): Lock {
        val fails = if (lock.lockedUntil in 1..now) 1 else lock.fails + 1
        return if (fails >= MAX_FAILS) Lock(0, now + LOCK_MINUTES * MINUTE) else Lock(fails, 0)
    }

    fun triesLeft(lock: Lock): Int = MAX_FAILS - lock.fails

    // 막아 둔 시간이 몇 분 남았는지 (올림)
    fun minutesLeft(lock: Lock, now: Long): Int = ((lock.lockedUntil - now + MINUTE - 1) / MINUTE).toInt()
}
