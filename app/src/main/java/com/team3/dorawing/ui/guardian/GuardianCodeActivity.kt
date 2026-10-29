package com.team3.dorawing.ui.guardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianCodeBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GuardianCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_guardian_code)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
