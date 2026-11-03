package com.team3.dorawing.util

import android.content.Context
import android.content.SharedPreferences
import com.team3.dorawing.R

object Prefs {
    private fun sp(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences("dorawing", Context.MODE_PRIVATE)

    // 이름을 아직 안 정했으면 null
    fun getName(context: Context): String? = sp(context).getString("name", null)
    fun setName(context: Context, name: String) = sp(context).edit().putString("name", name).apply()

    // 부를 때 쓰는 이름. 보통은 "김영희 님", 이름이 없거나 "어르신"이면 님 없이 "어르신"
    fun callName(context: Context, name: String? = getName(context)): String {
        val default = context.getString(R.string.name_default)
        return if (name.isNullOrBlank() || name == default) default else context.getString(R.string.name_call, name)
    }

    // 화면에 들어갈 때 자동으로 읽을지. 듣기 버튼은 이 값과 상관없이 읽는다
    fun isVoiceOn(context: Context): Boolean = sp(context).getBoolean("voice", true)
    fun setVoiceOn(context: Context, on: Boolean) = sp(context).edit().putBoolean("voice", on).apply()

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

    // 보호자 폰: 휴대폰 알림에 쓰는 어르신 호칭(예: 어머님). 서버에 올리지 않고 이 폰에만 둔다
    fun getElderCallName(context: Context): String =
        sp(context).getString("elderCallName", null) ?: context.getString(R.string.name_default)
    fun setElderCallName(context: Context, name: String) = sp(context).edit().putString("elderCallName", name).apply()

    // 보호자 폰: 이미 휴대폰 알림으로 띄운 변화 알림 (영역|날짜)
    fun getShownAlerts(context: Context): Set<String> = sp(context).getStringSet("shownAlerts", null)?.toSet() ?: emptySet()
    fun setShownAlerts(context: Context, keys: Set<String>) = sp(context).edit().putStringSet("shownAlerts", keys).apply()

    // 보호자 폰: 코드를 틀린 횟수와 입력을 막아 둔 시각
    fun getCodeFails(context: Context): Int = sp(context).getInt("codeFails", 0)
    fun getCodeLockedUntil(context: Context): Long = sp(context).getLong("codeLockedUntil", 0)
    fun setCodeLock(context: Context, fails: Int, lockedUntil: Long) =
        sp(context).edit().putInt("codeFails", fails).putLong("codeLockedUntil", lockedUntil).apply()
}
