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
        binding.tvProgress.text = getString(R.string.card_progress, index + 1, cards.size)
        binding.tvPicture.text = card.type
        binding.tvCardTitle.text = card.title
        binding.tvBody.text = card.body
        val more = if (card.link.isBlank()) View.GONE else View.VISIBLE
        binding.btnMore.visibility = more
        binding.tvMoreHelp.visibility = more

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
                link = item.optString("link")
            )
        }
    }
}
