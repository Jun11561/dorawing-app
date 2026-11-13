package com.team3.dorawing.ui.game

import android.view.View
import com.google.android.material.button.MaterialButton
import com.team3.dorawing.R
import com.team3.dorawing.databinding.ActivityGameLanguageBinding

// 언어력 게임: 낱말과 속담. 빈칸 채우기, 그림 보고 이름 고르기, 같은 종류가 아닌 것 고르기
class LanguageGameActivity : BaseGameActivity() {
    private lateinit var binding: ActivityGameLanguageBinding
    private var kind = ""
    private var problem = LanguageRules.Problem("", emptyList(), 0)
    private var buttons = listOf<MaterialButton>()

    override fun contentLayout() = R.layout.activity_game_language

    override fun bindContent(content: View) {
        binding = ActivityGameLanguageBinding.bind(content)
    }

    override fun makeProblems(level: Int) = LanguageRules.problems(level)

    override fun showProblem(kind: String, retry: Boolean) {
        this.kind = kind
        binding.tvPicture.visibility = View.GONE
        binding.tvPrompt.visibility = View.GONE
        val guide: String
        val speech: String
        when (kind) {
            LanguageRules.BLANK -> {
                problem = LanguageRules.makeBlank(level)
                binding.tvPrompt.visibility = View.VISIBLE
                binding.tvPrompt.text = problem.prompt
                guide = getString(R.string.game_lang_blank_guide)
                // 빈칸은 "땡"으로 읽는다
                speech = "$guide " + problem.prompt.replace("[    ]", "땡")
            }
            LanguageRules.NAME -> {
                problem = LanguageRules.makeName(level)
                binding.tvPicture.visibility = View.VISIBLE
                binding.tvPicture.text = problem.prompt
                guide = getString(R.string.game_lang_name_guide)
                speech = guide
            }
            else -> {
                problem = LanguageRules.makeGroup(level)
                binding.tvPrompt.visibility = View.VISIBLE
                binding.tvPrompt.text = getString(R.string.game_lang_group_q, problem.group)
                guide = getString(R.string.game_lang_group_guide)
                speech = binding.tvPrompt.text.toString() + " " + problem.choices.joinToString(", ")
            }
        }
        buttons = addChoices(binding.listChoices, problem.choices) { i, _ -> choose(i) }
        setGuide(guide, speech)
    }

    private fun choose(i: Int) {
        val answer = problem.choices[problem.answer]
        if (i == problem.answer) {
            markChoice(buttons[i], true)
            answered(true)
        } else {
            markChoice(buttons[problem.answer], true)
            val message = when (kind) {
                LanguageRules.BLANK -> getString(
                    R.string.game_lang_blank_wrong,
                    LanguageRules.josa("\"$answer\"", "이에요", "예요") + ". " + LanguageRules.fullProverb(problem.prompt, answer)
                )
                LanguageRules.NAME -> getString(R.string.game_lang_name_wrong, LanguageRules.josa(answer, "이에요", "예요"))
                else -> getString(R.string.game_lang_group_wrong, LanguageRules.josa(answer, "은", "는"), problem.group)
            }
            answered(false, message)
        }
    }

    // 힌트: 빈칸과 이름은 첫 글자를, 같은 종류는 틀린 보기 하나를 지워 준다
    override fun onHint() {
        val answer = problem.choices[problem.answer]
        when (kind) {
            LanguageRules.GROUP -> {
                val wrong = buttons.indices.filter { it != problem.answer }.random()
                buttons[wrong].isEnabled = false
                buttons[wrong].alpha = 0.4f
                say(getString(R.string.game_lang_hint_group, LanguageRules.josa(problem.choices[wrong], "은", "는"), problem.group))
            }
            else -> say(getString(R.string.game_lang_hint_first, LanguageRules.josa("\"${answer.first()}\"", "이에요", "예요")))
        }
    }
}
