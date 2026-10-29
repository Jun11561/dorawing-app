package com.team3.dorawing.ui.guardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianHomeBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GuardianHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_guardian_home)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
