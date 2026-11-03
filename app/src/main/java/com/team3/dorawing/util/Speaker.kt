package com.team3.dorawing.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.team3.dorawing.R
import java.util.Locale

// 앱 전체에서 하나만 쓰는 한국어 음성 안내
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var noticeShown = false
    private var pending: String? = null
    private val main = Handler(Looper.getMainLooper())

    // 화면에 들어갈 때 자동으로 읽는다. 설정에서 음성 안내를 끄면 읽지 않는다
    fun speak(context: Context, text: String) {
        if (!Prefs.isVoiceOn(context)) return
        speakNow(context, text)
    }

    // 듣기 버튼을 눌렀을 때. 음성 안내를 꺼 두었어도 읽는다
    fun speakNow(context: Context, text: String) {
        if (failed) return
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dorawing")
        } else {
            // 음성 엔진이 준비되면 마지막 문장만 읽는다
            pending = text
            start(context.applicationContext)
        }
    }

    fun stop() {
        pending = null
        if (ready) tts?.stop()
    }

    fun setVoiceOn(context: Context, on: Boolean) {
        Prefs.setVoiceOn(context, on)
        if (!on) stop()
    }

    private fun start(appContext: Context) {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            main.post {
                val result = if (status == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.KOREAN) else null
                if (result == null || result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 한국어 음성이 없어도 앱은 그대로 쓰고 한 번만 알려 준다
                    failed = true
                    tts?.shutdown()
                    tts = null
                    if (!noticeShown) {
                        noticeShown = true
                        Toast.makeText(appContext, R.string.no_korean_voice, Toast.LENGTH_LONG).show()
                    }
                } else {
                    // 어르신이 알아듣기 쉽게 조금 천천히
                    tts?.setSpeechRate(0.9f)
                    ready = true
                    pending?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "dorawing") }
                    pending = null
                }
            }
        }
    }
}
