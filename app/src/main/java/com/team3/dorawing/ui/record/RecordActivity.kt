package com.team3.dorawing.ui.record

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityRecordBinding
import com.team3.dorawing.db.AppDatabase
import com.team3.dorawing.db.ChangeAlert
import com.team3.dorawing.db.DiagnosisResult
import com.team3.dorawing.game.GameStats
import com.team3.dorawing.game.GameType
import com.team3.dorawing.ui.diagnosis.DiagnosisIntroActivity
import com.team3.dorawing.ui.diagnosis.DiagnosisResultActivity
import com.team3.dorawing.ui.diagnosis.DiagnosisScore
import com.team3.dorawing.ui.game.GameSelectActivity
import com.team3.dorawing.ui.guardian.GuardianRoleSelectActivity
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

class RecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordBinding
    private var speech = ""
    private var alerts = listOf<ChangeAlert>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TopBar.setup(this, binding.topBar, getString(R.string.title_record)) { speech }

        binding.btnAlertGo.setOnClickListener { startActivity(Intent(this, DiagnosisIntroActivity::class.java)) }
        binding.btnAlertLater.setOnClickListener { dismissAlerts() }
        binding.btnEmptyGo.setOnClickListener { startActivity(Intent(this, GameSelectActivity::class.java)) }
        binding.btnGuardian.setOnClickListener { startActivity(Intent(this, GuardianRoleSelectActivity::class.java)) }
        setupChart()
    }

    // 게임이나 자가진단을 하고 돌아오면 새로 읽는다
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { load() }
    }

    private suspend fun load() {
        val db = AppDatabase.get(this)
        val today = LocalDate.now()

        // 영역 수를 코드에 적지 않고 GameType 목록을 돈다
        val averages = GameType.entries.map { it to GameStats.recentAverage(this, it) }
        val played = averages.mapNotNull { (type, avg) -> avg?.let { type to it } }
        val weakest = played.minByOrNull { it.second }
        val best = played.maxByOrNull { it.second }
        // 모두 같은 정답률이면 약한 영역이 없다
        val lowest = if (weakest != null && best != null && weakest.second < best.second) weakest.first else null
        val unplayed = averages.filter { it.second == null }.map { it.first }

        // 지금 GameType에 없는 영역의 알림은 건너뛴다
        alerts = db.changeAlertDao().active().filter { GameType.fromName(it.gameType) != null }

        val monday = Dates.monday(today)
        val attended = db.attendanceDao()
            .between(Dates.dayText(monday), Dates.dayText(monday.plusDays(6)))
            .filter { it.completedSets > 0 }
            .map { it.date }
            .toSet()

        val lastDiagnosis = db.diagnosisDao().last()

        showAlerts()
        if (weakest == null) {
            binding.boxEmpty.visibility = View.VISIBLE
            binding.boxChart.visibility = View.GONE
        } else {
            binding.boxEmpty.visibility = View.GONE
            binding.boxChart.visibility = View.VISIBLE
            showChart(averages, lowest)
            // 약한 영역이 있으면 그 영역, 없으면 아직 안 해 본 영역을 권한다
            when {
                lowest != null -> showRecommend(lowest, false)
                unplayed.isNotEmpty() -> showRecommend(unplayed.first(), true)
                else -> showRecommend(weakest.first, false)
            }
        }
        showWeek(monday, today, attended)
        showDiagnosis(lastDiagnosis)

        // 들어오면 요약을 읽어 준다
        val parts = mutableListOf<String>()
        if (alerts.isNotEmpty()) parts += getString(R.string.record_alert)
        if (best == null || weakest == null) {
            parts += getString(R.string.record_empty)
        } else {
            parts += getString(R.string.record_week_count, attended.size)
            if (lowest != null) {
                parts += getString(R.string.record_speech_best, best.first.label)
                parts += getString(R.string.record_speech_weak, lowest.label)
            }
        }
        speech = parts.joinToString(" ")
        Speaker.speak(this, speech)
    }

    private fun showAlerts() {
        if (alerts.isEmpty()) {
            binding.cardAlert.visibility = View.GONE
            return
        }
        binding.cardAlert.visibility = View.VISIBLE
        val names = alerts.mapNotNull { GameType.fromName(it.gameType)?.label }.distinct()
        binding.tvAlertAreas.text = getString(R.string.record_alert_areas, names.joinToString(", "))
    }

    // 나중에: 지금 보이는 알림을 모두 닫는다
    private fun dismissAlerts() {
        val shown = alerts
        alerts = emptyList()
        binding.cardAlert.visibility = View.GONE
        lifecycleScope.launch {
            val dao = AppDatabase.get(this@RecordActivity).changeAlertDao()
            shown.forEach { dao.dismiss(it.id) }
        }
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setFitBars(true)
            extraBottomOffset = 8f
            // 양옆 여백을 줄여 막대 칸을 넓힌다
            minOffset = 4f
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 25f
                // 막대 위에 숫자가 있으니 작은 축 글자는 빼고 눈금선만 둔다
                setDrawLabels(false)
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = color(R.color.text_main)
            }
        }
    }

    // 그래프 글자는 dp라서 폰의 글자 크기 배율을 직접 곱한다.
    // 다만 옆 막대 글자와 겹치지 않게 막대 한 칸 너비에 들어가는 만큼까지만 키운다
    private fun chartText(texts: List<String>, count: Int): Float {
        val want = 20f * resources.configuration.fontScale
        val width = binding.chart.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        // 양옆 여백을 빼고, 옆 칸 글자와 붙지 않게 칸의 85%만 쓴다
        val slot = (width / resources.displayMetrics.density - 8f) / count * 0.85f
        // 글자 크기 100일 때 너비를 재서 비례로 계산한다
        val paint = Paint().apply { textSize = 100f }
        return min(want, slot * 100f / texts.maxOf { paint.measureText(it) })
    }

    // 영역별 최근 3세트 평균. 가장 낮은 영역만 코랄색
    private fun showChart(averages: List<Pair<GameType, Float?>>, lowest: GameType?) {
        val entries = averages.mapIndexed { i, (_, avg) ->
            // 기록이 없는 영역은 막대 없이 비워 두고 아래 글로 알려 준다
            BarEntry(i.toFloat(), (avg ?: 0f) * 100f, avg)
        }
        val dataSet = BarDataSet(entries, "").apply {
            colors = averages.map { (type, _) -> color(if (type == lowest) R.color.coral else R.color.green_mid) }
            valueTextSize = chartText(listOf("100%"), averages.size)
            valueTextColor = color(R.color.text_main)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry): String =
                    if (barEntry.data == null) "" else "${barEntry.y.roundToInt()}%"
            }
        }
        val empty = averages.filter { it.second == null }.joinToString(", ") { it.first.label }
        binding.tvNoSets.visibility = if (empty.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoSets.text = getString(R.string.record_no_sets, empty)
        val labels = averages.map { it.first.label }
        binding.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        // 라이브러리가 축 글자는 24까지만 키운다
        binding.chart.xAxis.textSize = chartText(labels, labels.size)
        // 100% 막대 위 숫자가 잘리지 않게 위쪽 자리
        binding.chart.extraTopOffset = dataSet.valueTextSize / resources.displayMetrics.density
        binding.chart.xAxis.labelCount = averages.size
        binding.chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        binding.chart.invalidate()
    }

    // 약한 영역 추천은 게임 화면이 아니라 게임 선택 화면을 연다
    private fun showRecommend(type: GameType, isNew: Boolean) {
        binding.btnRecommend.text = getString(if (isNew) R.string.record_recommend_new else R.string.record_recommend, type.label)
        binding.btnRecommend.setOnClickListener {
            startActivity(Intent(this, GameSelectActivity::class.java).putExtra("recommend", type.name))
        }
    }

    // 월~일 7칸. 출석한 날은 도장
    private fun showWeek(monday: LocalDate, today: LocalDate, attended: Set<String>) {
        binding.weekRow.removeAllViews()
        val circle = resources.getDimensionPixelSize(R.dimen.stamp_small)
        for (i in 0 until 7) {
            val day = monday.plusDays(i.toLong())
            val done = Dates.dayText(day) in attended

            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val label = TextView(this).apply {
                text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                gravity = Gravity.CENTER
                // 오늘은 굵은 코랄색
                if (day == today) {
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.coral_dark))
                }
            }
            val stamp = TextView(this).apply {
                gravity = Gravity.CENTER
                text = if (done) "✔" else ""
                setTextColor(color(R.color.coral_dark))
                setBackgroundResource(if (done) R.drawable.bg_stamp else R.drawable.bg_stamp_empty)
            }
            cell.addView(label)
            cell.addView(stamp, LinearLayout.LayoutParams(circle, circle))
            binding.weekRow.addView(cell, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        binding.tvWeekCount.text = getString(R.string.record_week_count, attended.size)
    }

    private fun showDiagnosis(last: DiagnosisResult?) {
        if (last == null) {
            binding.tvDiagDate.visibility = View.GONE
            binding.tvDiagResult.setText(R.string.record_diag_none)
            binding.btnDiag.setText(R.string.record_diag_go)
            binding.btnDiag.setOnClickListener { startActivity(Intent(this, DiagnosisIntroActivity::class.java)) }
            return
        }
        binding.tvDiagDate.visibility = View.VISIBLE
        binding.tvDiagDate.text = Dates.koreanDay(last.takenAt)
        val judge = getString(if (last.isSuspected) R.string.result_suspected else R.string.result_normal)
        binding.tvDiagResult.text = getString(R.string.result_judge, judge, last.totalScore, DiagnosisScore.MAX_SCORE)
        binding.btnDiag.setText(R.string.record_diag_again)
        // 다시 볼 때는 저장이나 시작 레벨 설정을 하지 않는다
        binding.btnDiag.setOnClickListener { startActivity(DiagnosisResultActivity.pastIntent(this, last)) }
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)
}
