package com.team3.dorawing.util

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ViewTopBarBinding
import com.team3.dorawing.ui.home.HomeActivity

// 모든 화면이 onCreate에서 부른다.
// 제목, 듣기 버튼, 홈 버튼, 상태 바 여백, 화면을 떠날 때 음성 멈추기를 한 번에 처리한다
object TopBar {

    fun setup(
        activity: AppCompatActivity,
        bar: ViewTopBarBinding,
        title: String,
        showHome: Boolean = true,
        // 홈 버튼을 눌렀을 때 바로 가지 않고 먼저 물어볼 화면은 따로 넘긴다
        onHome: (() -> Unit)? = null,
        speech: () -> String = { title }
    ) {
        bar.tvTitle.text = title
        // 음성 안내를 꺼도 듣기 버튼은 읽어 준다. 읽는 동안에는 멈추기 버튼이 된다
        bar.btnSpeak.setOnClickListener {
            if (Speaker.speaking) Speaker.stop() else Speaker.speakNow(activity, speech())
        }

        bar.btnHome.visibility = if (showHome) View.VISIBLE else View.GONE
        bar.btnHome.setOnClickListener { if (onHome != null) onHome() else goHome(activity) }

        // 화면이 상태 바, 내비게이션 바, 키보드에 가리지 않게
        val content = activity.findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // 보이는 동안만 읽는 중인지 듣고, 다음 화면의 안내와 겹치지 않게 떠날 때 멈춘다
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = Speaker.watch { speaking -> showSpeaking(bar, speaking) }
            override fun onPause(owner: LifecycleOwner) {
                Speaker.watch(null)
                Speaker.stop()
            }
        })
    }

    private fun showSpeaking(bar: ViewTopBarBinding, speaking: Boolean) {
        bar.btnSpeak.setText(if (speaking) R.string.top_stop else R.string.top_speak)
        bar.btnSpeak.setIconResource(if (speaking) R.drawable.ic_stop else R.drawable.ic_speaker)
    }

    // 쌓여 있던 화면을 모두 닫고 홈으로
    fun goHome(activity: AppCompatActivity) {
        val intent = Intent(activity, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
        activity.finish()
    }
}
