package com.team3.dorawing.ui.game

import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameAttentionBinding

// 주의력 게임: 색깔 글자 맞추기. 글자의 뜻과 색이 다를 때 색만 보고 고르는 연습 (스트룹 검사 방식)
class AttentionGameActivity : BaseGameActivity() {
    private lateinit var binding: ActivityGameAttentionBinding
    private var kind = ""
    private var problem = AttentionRules.Problem(0, 0, false, 0)
    private var buttons = listOf<MaterialButton>()

    override fun contentLayout() = R.layout.activity_game_attention

    override fun bindContent(content: View) {
        binding = ActivityGameAttentionBinding.bind(content)
    }

    override fun makeProblems(level: Int) = AttentionRules.problems(level)

    override fun showProblem(kind: String, retry: Boolean) {
        this.kind = kind
        problem = AttentionRules.make(kind, level)
        val colors = AttentionRules.COLORS

        // 큰 글자: 뜻은 word, 칠한 색은 ink
        binding.tvWord.text = colors[problem.word].first
        binding.tvWord.setTextColor(colors[problem.ink].second)
        binding.wordFrame.background =
            if (problem.framed) getDrawable(R.drawable.game_word_frame) else null

        // 보기: 색 이름과 색 점. 두 개씩 한 줄
        val n = AttentionRules.colorCount(level)
        buttons = addChoices(binding.listChoices, colors.take(n).map { it.first }, perRow = 2) { i, _ -> choose(i) }
        buttons.forEachIndexed { i, button ->
            button.setIconResource(R.drawable.ic_radio_on)
            button.iconTint = ColorStateList.valueOf(colors[i].second)
        }

        when (kind) {
            AttentionRules.SAME -> setGuide(getString(R.string.game_att_same_guide), getString(R.string.game_att_same_speech))
            AttentionRules.DIFF -> setGuide(getString(R.string.game_att_diff_guide), getString(R.string.game_att_diff_speech))
            else -> setGuide(getString(R.string.game_att_switch_guide), getString(R.string.game_att_switch_speech))
        }
    }

    private fun choose(i: Int) {
        val name = AttentionRules.COLORS[problem.answer].first
        if (i == problem.answer) {
            markChoice(buttons[i], true)
            answered(true)
        } else {
            // 정답 보기를 초록으로 보여 준다
            markChoice(buttons[problem.answer], true)
            val message = if (kind == AttentionRules.SWITCH && problem.framed) {
                getString(R.string.game_att_wrong_framed, name)
            } else {
                getString(R.string.game_att_wrong_ink, name)
            }
            answered(false, message)
        }
    }

    // 힌트: 지금 어떤 규칙으로 봐야 하는지만 알려 준다. 답은 말하지 않는다
    override fun onHint() {
        val text = when {
            kind == AttentionRules.SWITCH && problem.framed -> getString(R.string.game_att_hint_framed)
            kind == AttentionRules.SWITCH -> getString(R.string.game_att_hint_noframe)
            else -> getString(R.string.game_att_hint_ink)
        }
        say(text)
    }
}
