package com.team3.dorawing.ui.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameSelectBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GameSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.game_title_select)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
