package com.team3.dorawing.ui.guardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianConsentBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GuardianConsentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianConsentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_guardian_consent)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
