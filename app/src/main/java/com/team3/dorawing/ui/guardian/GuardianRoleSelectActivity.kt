package com.team3.dorawing.ui.guardian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGuardianRoleSelectBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class GuardianRoleSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuardianRoleSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianRoleSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_guardian_role)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
