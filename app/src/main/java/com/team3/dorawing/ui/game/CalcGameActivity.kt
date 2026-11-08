package com.team3.dorawing.ui.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameCalcBinding
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 껍데기 화면. 내용은 허시온이 만든다
class CalcGameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameCalcBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameCalcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = GameType.fromName(intent.getStringExtra("gameType") ?: "") ?: GameType.CALC
        val level = intent.getIntExtra("level", 1)
        val title = getString(R.string.game_title_play, type.label, level)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
