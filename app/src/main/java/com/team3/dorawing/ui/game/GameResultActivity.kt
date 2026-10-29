package com.team3.dorawing.ui.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameResultBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GameResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.game_title_result)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
