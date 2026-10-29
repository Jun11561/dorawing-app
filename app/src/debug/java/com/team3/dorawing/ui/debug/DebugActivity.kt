package com.team3.dorawing.ui.debug

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityDebugBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.game.GameRecorder
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.ui.game.GameLauncher
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

// 디버그 빌드에만 들어가는 화면. 가상 기록을 넣고 게임 화면을 바로 띄운다
class DebugActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDebugBinding
    private val buttons = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TopBar.setup(this, binding.topBar, getString(R.string.title_debug))

        addButton(binding.scenarioList, "1. 5주 꾸준히 보통") { steady() }
        addButton(binding.scenarioList, "2. 최근 1주 ${GameType.entries.first().label} 하락") { oneDrop() }
        addButton(binding.scenarioList, "3. 오늘 2세트") { todayTwo() }
        addButton(binding.scenarioList, "전부 지우기") { clearAll() }

        for (type in GameType.entries) {
            addButton(binding.gameList, "${type.label} Lv.3 바로 열기", runInBackground = false) {
                startActivity(GameLauncher.intentFor(this, type, 3))
            }
        }

        lifecycleScope.launch { showStatus() }
    }

    // 1, 2번은 기존 기록을 지우고 넣는다
    private suspend fun steady() {
        clearAll()
        DebugScenarios.steady().forEach { record(it) }
    }

    private suspend fun oneDrop() {
        clearAll()
        DebugScenarios.oneDrop().forEach { record(it) }
    }

    private suspend fun todayTwo() {
        DebugScenarios.todayTwo().forEach { record(it) }
    }

    private suspend fun clearAll() {
        withContext(Dispatchers.IO) { AppDatabase.get(this@DebugActivity).clearAllTables() }
    }

    private suspend fun record(set: PlannedSet) {
        // 오늘 기록은 지금 시각, 과거 기록은 그날 오전 10시부터 영역마다 10분씩
        val playedAt = if (set.daysAgo == 0) {
            System.currentTimeMillis()
        } else {
            Dates.toMillis(LocalDate.now().minusDays(set.daysAgo.toLong())) + (10 * 60 + set.order * 10) * 60_000L
        }
        val level = GameStats.currentLevel(this, set.type)
        GameRecorder.finish(this, set.type, level, set.correct, DebugScenarios.TOTAL, 1500, playedAt)
    }

    private suspend fun showStatus() {
        val db = AppDatabase.get(this)
        val lines = mutableListOf("세트 기록 ${db.gameSetDao().count()}개")
        for (type in GameType.entries) {
            val state = db.levelStateDao().get(type.name)
            lines += if (state == null) "${type.label}: 기록 없음"
            else "${type.label}: Lv.${state.level}, 전체 ${state.totalSets}세트, 레벨 변경 뒤 ${state.setsSinceLevelChange}세트"
        }
        val alerts = db.changeAlertDao().active()
        val names = alerts.mapNotNull { GameType.fromName(it.gameType)?.label }
        lines += "변화 알림 ${alerts.size}개 ${if (names.isEmpty()) "" else names.joinToString(prefix = "(", postfix = ")")}"
        binding.tvStatus.text = lines.joinToString("\n")
    }

    private fun addButton(list: LinearLayout, text: String, runInBackground: Boolean = true, action: suspend () -> Unit) {
        val button = MaterialButton(this)
        button.text = text
        button.setOnClickListener {
            if (!runInBackground) {
                lifecycleScope.launch { action() }
                return@setOnClickListener
            }
            // 넣는 동안 다른 버튼을 못 누르게
            buttons.forEach { it.isEnabled = false }
            binding.tvStatus.text = "넣는 중..."
            lifecycleScope.launch {
                action()
                showStatus()
                buttons.forEach { it.isEnabled = true }
            }
        }
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.topMargin = resources.getDimensionPixelSize(R.dimen.gap)
        list.addView(button, params)
        buttons += button
    }
}
