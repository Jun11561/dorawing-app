package com.team3.dorawing.ui.guardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianEnterCodeBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GuardianEnterCodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianEnterCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianEnterCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_guardian_enter_code)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
