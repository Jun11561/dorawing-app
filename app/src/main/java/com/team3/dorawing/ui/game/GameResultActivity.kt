package com.team3.dorawing.ui.game

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameResultBinding
import com.team3.dorawing.game.FinishSummary
import com.team3.dorawing.game.GameType
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar

// 세트가 끝난 뒤 결과. 받는 값: summary(FinishSummary), gameType(이름), bestKind(오늘 가장 잘한 유형, 없으면 null)
class GameResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 새 함수(IntentCompat)는 지금 core 버전에 없어서 예전 방식으로 읽는다
        @Suppress("DEPRECATION")
        val summary = intent.getSerializableExtra("summary") as? FinishSummary
        if (summary == null) {
            finish()
            return
        }
        binding = ActivityGameResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = GameType.fromName(intent.getStringExtra("gameType") ?: "")
        val best = intent.getStringExtra("bestKind")

        val judge = getString(
            when (summary.stars) {
                3 -> R.string.game_result_great
                2 -> R.string.game_result_good
                else -> R.string.game_result_try
            }
        )
        val score = getString(R.string.game_result_score, summary.total, summary.correct)
        val levelText = when {
            summary.newLevel > summary.oldLevel -> getString(R.string.game_result_level_up, summary.oldLevel, summary.newLevel)
            summary.newLevel < summary.oldLevel -> getString(R.string.game_result_level_down, summary.newLevel)
            else -> getString(R.string.game_result_level_same, summary.newLevel)
        }

        binding.tvJudge.text = judge
        binding.tvStars.text = "★".repeat(summary.stars) + "☆".repeat(3 - summary.stars)
        binding.tvScore.text = score
        binding.tvLevel.text = levelText
        if (best == null) {
            binding.tvBest.visibility = View.GONE
        } else {
            binding.tvBest.text = getString(R.string.game_result_best, best)
        }

        var speech = "$judge $score $levelText"
        if (best != null) speech += " " + getString(R.string.game_result_best, best)
        TopBar.setup(this, binding.topBar, getString(R.string.game_title_result)) { speech }
        Speaker.speak(this, speech)

        // 한 번 더: 새 레벨로 같은 게임. 게임 종류를 모르면 선택 화면으로
        binding.btnAgain.setOnClickListener {
            if (type != null) startActivity(GameLauncher.intentFor(this, type, summary.newLevel))
            else startActivity(
                Intent(this, GameSelectActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }
        // 이미 열려 있던 게임 선택 화면으로 돌아간다. 매번 새로 열면 뒤로 가기를 여러 번 눌러야 홈이 나온다
        binding.btnOther.setOnClickListener {
            startActivity(
                Intent(this, GameSelectActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        }
    }
}
