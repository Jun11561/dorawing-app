package com.team3.dorawing.ui.record

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityRecordBinding
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

class RecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = getString(R.string.title_record)
        TopBar.setup(this, binding.topBar, title)
        Speaker.speak(this, title)
    }
}
