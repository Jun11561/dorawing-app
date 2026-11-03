package com.team3.dorawing.ui.guardian

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianConsentBinding
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 어르신: 무엇이 공유되고 무엇이 안 되는지 보고 동의한다
class GuardianConsentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianConsentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val speech = getString(R.string.consent_speech)
        TopBar.setup(this, binding.topBar, getString(R.string.title_guardian_consent)) { speech }
        Speaker.speak(this, speech)

        binding.btnAgree.setOnClickListener {
            Prefs.setConsentDone(this, true)
            startActivity(Intent(this, GuardianCodeActivity::class.java))
            finish()
        }
        binding.btnLater.setOnClickListener { finish() }
    }
}
