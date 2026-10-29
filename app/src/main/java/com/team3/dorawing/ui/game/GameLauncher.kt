package com.team3.dorawing.ui.game

import android.content.Context
import android.content.Intent
import com.team3.dorawing.game.GameType

// 게임 화면을 여는 유일한 함수. 게임 밖의 코드는 이것만 부른다
object GameLauncher {

    fun intentFor(context: Context, type: GameType, level: Int): Intent {
        // GameType에 게임을 추가하면 여기서 컴파일 오류가 나므로 같이 고친다
        val screen = when (type) {
            GameType.MEMORY -> MemoryGameActivity::class.java
            GameType.CALC -> CalcGameActivity::class.java
            GameType.ATTENTION -> AttentionGameActivity::class.java
            GameType.LANGUAGE -> LanguageGameActivity::class.java
        }
        return Intent(context, screen)
            .putExtra("gameType", type.name)
            .putExtra("level", level)
    }
}
