package com.team3.dorawing.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    // 읽는 중인지. 상단 바의 듣기 버튼이 이 값에 따라 멈추기로 바뀐다
    var speaking = false
        private set
    private var onSpeaking: ((Boolean) -> Unit)? = null

    // 문장마다 번호를 붙여서, 새 문장으로 바뀔 때 앞 문장이 끝났다는 알림은 무시한다
    private var lastId = 0
    private var currentId = ""

    // 화면에 들어갈 때 자동으로 읽는다. 설정에서 음성 안내를 끄면 읽지 않는다
    fun speak(context: Context, text: String) {
        if (!Prefs.isVoiceOn(context)) return
        speakNow(context, text)
    }

    // 듣기 버튼을 눌렀을 때. 음성 안내를 꺼 두었어도 읽는다
    fun speakNow(context: Context, text: String) {
        if (failed) return
        if (ready) {
            say(text)
        } else {
            // 음성 엔진이 준비되면 마지막 문장만 읽는다
            pending = text
            start(context.applicationContext)
        }
    }

    fun stop() {
        pending = null
        currentId = ""
        if (ready) tts?.stop()
        if (speaking) {
            speaking = false
            onSpeaking?.invoke(false)
        }
    }

    // 지금 보이는 화면의 상단 바가 읽는 중인지 듣는다. 화면을 떠날 때 null
    fun watch(listener: ((Boolean) -> Unit)?) {
        onSpeaking = listener
        listener?.invoke(speaking)
    }

    private fun say(text: String) {
        lastId++
        currentId = "dorawing$lastId"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, currentId)
    }

    // 음성 엔진은 다른 스레드에서 알려 주므로 화면 스레드로 옮긴다
    private fun changed(id: String?, value: Boolean) {
        main.post {
            if (id != currentId || speaking == value) return@post
            speaking = value
            onSpeaking?.invoke(value)
        }
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
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = changed(utteranceId, true)
                        override fun onDone(utteranceId: String?) = changed(utteranceId, false)
                        override fun onStop(utteranceId: String?, interrupted: Boolean) = changed(utteranceId, false)
                        override fun onError(utteranceId: String?, errorCode: Int) = changed(utteranceId, false)
                        @Deprecated("음성 엔진이 아직 부르는 옛 함수라 같이 둔다")
                        override fun onError(utteranceId: String?) = changed(utteranceId, false)
                    })
                    ready = true
                    pending?.let { say(it) }
                    pending = null
                }
            }
        }
    }
}
