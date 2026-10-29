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
}
