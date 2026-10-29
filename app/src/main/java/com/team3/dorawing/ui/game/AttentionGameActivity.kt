package com.team3.dorawing.ui.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.databinding.ActivityGameAttentionBinding
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 껍데기 화면. 내용은 허시온이 만든다
class AttentionGameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameAttentionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameAttentionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = GameType.fromName(intent.getStringExtra("gameType") ?: "") ?: GameType.ATTENTION
        val level = intent.getIntExtra("level", 1)
        val title = "${type.label} 훈련 Lv.$level"
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
