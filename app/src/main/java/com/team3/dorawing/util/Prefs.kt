package com.team3.dorawing.util

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val TEXT_LARGE = 0
    const val TEXT_XLARGE = 1

    private fun sp(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("dorawing", Context.MODE_PRIVATE)

    // 이름을 아직 안 정했으면 null
    fun getName(context: Context): String? = sp(context).getString("name", null)
    fun setName(context: Context, name: String) = sp(context).edit().putString("name", name).apply()

    fun isVoiceOn(context: Context): Boolean = sp(context).getBoolean("voice", true)
    fun setVoiceOn(context: Context, on: Boolean) = sp(context).edit().putBoolean("voice", on).apply()

    fun getTextSize(context: Context): Int = sp(context).getInt("textSize", TEXT_LARGE)
    fun setTextSize(context: Context, size: Int) = sp(context).edit().putInt("textSize", size).apply()

    fun isGuardianMode(context: Context): Boolean = sp(context).getBoolean("guardianMode", false)
    fun setGuardianMode(context: Context, on: Boolean) = sp(context).edit().putBoolean("guardianMode", on).apply()

    // 건강 카드를 끝까지 본 날짜 (yyyy-MM-dd)
    fun getCardsDoneDay(context: Context): String? = sp(context).getString("cardsDoneDay", null)
    fun setCardsDoneDay(context: Context, day: String) = sp(context).edit().putString("cardsDoneDay", day).apply()

    // 보호자 연결 (어르신 폰)
    fun isConsentDone(context: Context): Boolean = sp(context).getBoolean("consentDone", false)
    fun setConsentDone(context: Context, done: Boolean) = sp(context).edit().putBoolean("consentDone", done).apply()

    // 이 폰이 어르신으로 연결할 때 쓴 익명 uid. 다시 설치해서 uid가 바뀌었는지 볼 때 쓴다
    fun getElderUid(context: Context): String? = sp(context).getString("elderUid", null)
    fun setElderUid(context: Context, uid: String) = sp(context).edit().putString("elderUid", uid).apply()

    // 서버에 보호자나 요약이 있을 수 있으면 true. false면 서버에 아무것도 올리지 않는다
    fun hasGuardian(context: Context): Boolean = sp(context).getBoolean("hasGuardian", false)
    fun setHasGuardian(context: Context, has: Boolean) = sp(context).edit().putBoolean("hasGuardian", has).apply()

    // 인터넷이 없어서 못 올린 요약이 있으면 true
    fun isUploadPending(context: Context): Boolean = sp(context).getBoolean("uploadPending", false)
    fun setUploadPending(context: Context, pending: Boolean) = sp(context).edit().putBoolean("uploadPending", pending).apply()

    // 지금 보여 주는 연결 코드와 끝나는 시각
    fun getCode(context: Context): String? = sp(context).getString("pairCode", null)
    fun getCodeExpires(context: Context): Long = sp(context).getLong("pairCodeExpires", 0)
    fun setCode(context: Context, code: String, expiresAt: Long) =
        sp(context).edit().putString("pairCode", code).putLong("pairCodeExpires", expiresAt).apply()
    fun clearCode(context: Context) = sp(context).edit().remove("pairCode").remove("pairCodeExpires").apply()

    // 보호자 호칭은 서버에 두지 않고 승인한 어르신 폰에만 적어 둔다
    fun getGuardianName(context: Context, uid: String): String? = sp(context).getString("guardianName_$uid", null)
    fun setGuardianName(context: Context, uid: String, name: String) =
        sp(context).edit().putString("guardianName_$uid", name).apply()
    fun removeGuardianName(context: Context, uid: String) = sp(context).edit().remove("guardianName_$uid").apply()

    // 보호자 폰: 연결된 어르신, 승인을 기다리는 어르신
    fun getLinkedElder(context: Context): String? = sp(context).getString("linkedElder", null)
    fun setLinkedElder(context: Context, uid: String?) = sp(context).edit().putString("linkedElder", uid).apply()
    fun getPendingElder(context: Context): String? = sp(context).getString("pendingElder", null)
    fun setPendingElder(context: Context, uid: String?) = sp(context).edit().putString("pendingElder", uid).apply()

    // 보호자 폰: 코드를 틀린 횟수와 입력을 막아 둔 시각
    fun getCodeFails(context: Context): Int = sp(context).getInt("codeFails", 0)
    fun getCodeLockedUntil(context: Context): Long = sp(context).getLong("codeLockedUntil", 0)
    fun setCodeLock(context: Context, fails: Int, lockedUntil: Long) =
        sp(context).edit().putInt("codeFails", fails).putLong("codeLockedUntil", lockedUntil).apply()
}
