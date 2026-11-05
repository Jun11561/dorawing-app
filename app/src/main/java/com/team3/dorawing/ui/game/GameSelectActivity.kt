package com.team3.dorawing.ui.game

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameSelectBinding
import com.team3.dorawing.databinding.ActivityGameSelectRowBinding
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch

// 게임 선택. 영역 목록은 GameType을 돌면서 그린다.
// 내 기록의 약한 영역 추천이 Intent extra "recommend"(GameType 이름)를 넘기면 그 영역에 "오늘 추천"을 붙인다
class GameSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameSelectBinding
    private var recommend: GameType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recommend = GameType.fromName(intent.getStringExtra("recommend") ?: "")
        var speech = getString(R.string.game_select_guide)
        recommend?.let { speech += " " + getString(R.string.game_select_recommend_speech, it.label) }

        TopBar.setup(this, binding.topBar, getString(R.string.game_title_select)) { speech }
        Speaker.speak(this, speech)
    }

    // 결과 화면에서 돌아오면 레벨이 바뀌어 있을 수 있어서 보일 때마다 다시 읽는다
    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            binding.listGames.removeAllViews()
            for (type in GameType.entries) {
                val level = GameStats.currentLevel(this@GameSelectActivity, type)
                val rate = GameStats.recentAverage(this@GameSelectActivity, type)
                val row = ActivityGameSelectRowBinding.inflate(layoutInflater, binding.listGames, false)
                row.tvName.text = type.label
                row.tvInfo.text = if (rate == null) {
                    getString(R.string.game_select_norecord, level)
                } else {
                    getString(R.string.game_select_rate, level, Math.round(rate * 100))
                }
                row.tvRecommend.visibility = if (type == recommend) View.VISIBLE else View.GONE
                row.root.setOnClickListener {
                    startActivity(GameLauncher.intentFor(this@GameSelectActivity, type, level))
                }
                binding.listGames.addView(row.root)
            }
        }
    }
}
