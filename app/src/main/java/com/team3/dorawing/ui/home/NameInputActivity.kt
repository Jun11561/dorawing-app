package com.team3.dorawing.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityNameInputBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class NameInputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNameInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_name_input)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
