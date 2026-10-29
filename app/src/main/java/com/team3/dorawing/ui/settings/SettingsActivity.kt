package com.team3.dorawing.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivitySettingsBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_settings)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
