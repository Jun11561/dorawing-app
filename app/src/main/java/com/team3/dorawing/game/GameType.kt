package com.team3.dorawing.game

// 게임 종류 목록. 게임을 늘리거나 빼면 이 파일만 고친다
enum class GameType(val label: String) {
    MEMORY("기억력"),
    CALC("계산력"),
    ATTENTION("주의력"),
    LANGUAGE("언어력");

    companion object {
        // DB에 남은 이름이 지금 목록에 없으면 null
        fun fromName(name: String): GameType? = entries.firstOrNull { it.name == name }
    }
}
