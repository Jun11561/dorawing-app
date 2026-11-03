package com.team3.dorawing.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityHealthCardBinding
import com.team3.dorawing.util.Dates
import com.team3.dorawing.util.Prefs
import com.team3.dorawing.util.Speaker
import com.team3.dorawing.util.TopBar
import org.json.JSONArray
import java.time.LocalDate

// 오늘의 건강 카드. 날짜에 따라 고른 카드를 한 장씩 넘겨 본다
class HealthCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHealthCardBinding
    private var cards = listOf<HealthCard>()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val all = loadCards()
        cards = HealthCards.pickToday(all.size, LocalDate.now()).map { all[it] }
        index = savedInstanceState?.getInt("index") ?: 0

        TopBar.setup(this, binding.topBar, getString(R.string.home_cards)) { cardSpeech() }

        binding.btnNext.setOnClickListener {
            if (index < cards.size - 1) {
                index++
                showCard()
            } else {
                finish()
            }
        }
        binding.btnMore.setOnClickListener { openLink() }
        binding.btnCall.setOnClickListener { call() }

        if (cards.isEmpty()) {
            binding.tvCardTitle.setText(R.string.card_empty)
            binding.btnMore.visibility = View.GONE
            binding.tvMoreHelp.visibility = View.GONE
            binding.btnNext.setText(R.string.card_finish)
            return
        }
        showCard()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
    }

    private fun showCard() {
        val card = cards[index]
        // 몇 번째 장인지는 제목 줄에 붙여 카드 아래 버튼과 안내까지 한 화면에 들어가게 한다
        binding.topBar.tvTitle.text = getString(R.string.card_progress, index + 1, cards.size)
        binding.tvPicture.text = card.type
        val icon = HealthCards.iconFor(card.icon)
        if (icon != null) binding.ivIcon.setImageResource(icon)
        binding.ivIcon.visibility = if (icon == null) View.GONE else View.VISIBLE
        binding.tvCardTitle.text = card.title
        binding.tvBody.text = card.body
        val more = if (card.link.isBlank()) View.GONE else View.VISIBLE
        binding.btnMore.visibility = more
        binding.tvMoreHelp.visibility = more
        // 무엇이 열리는지 버튼 이름으로 알려 주고, 안내는 한 줄로 둔다
        binding.btnMore.setText(if (HealthCards.isVideo(card.link)) R.string.card_more_video else R.string.card_more)
        binding.btnCall.visibility = if (card.phone.isBlank()) View.GONE else View.VISIBLE

        val last = index == cards.size - 1
        binding.btnNext.setText(if (last) R.string.card_finish else R.string.card_next)
        // 마지막 장까지 보면 오늘 도장
        if (last) Prefs.setCardsDoneDay(this, Dates.dayText(LocalDate.now()))

        Speaker.speak(this, cardSpeech())
    }

    private fun cardSpeech(): String {
        val card = cards.getOrNull(index) ?: return getString(R.string.card_empty)
        return "${card.type}. ${card.title}. ${card.body}"
    }

    // 자가진단 결과 화면과 같이 전화 앱에 번호만 넣어 연다 (전화 권한 필요 없음)
    private fun call() {
        val phone = cards.getOrNull(index)?.phone ?: return
        try {
            startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.result_no_phone, Toast.LENGTH_LONG).show()
        }
    }

    private fun openLink() {
        val link = cards.getOrNull(index)?.link ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.card_no_browser, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadCards(): List<HealthCard> {
        val text = resources.openRawResource(R.raw.health_cards).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return (0 until array.length()).map {
            val item = array.getJSONObject(it)
            HealthCard(
                type = item.optString("type"),
                title = item.optString("title"),
                body = item.optString("body"),
                link = item.optString("link"),
                icon = item.optString("icon"),
                phone = item.optString("phone")
            )
        }
    }
}
