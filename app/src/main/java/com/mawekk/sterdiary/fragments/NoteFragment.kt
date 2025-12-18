package com.mawekk.sterdiary.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.NoteWorker
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.databinding.FragmentNewNoteBinding
import com.mawekk.sterdiary.db.DiaryViewModel


class NoteFragment : Fragment() {
    private lateinit var binding: FragmentNewNoteBinding
    private lateinit var worker: NoteWorker
    private val viewModel: DiaryViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewNoteBinding.inflate(inflater, container, false)
        worker = NoteWorker(
            requireActivity(),
            activity as MainActivity,
            viewModel,
            viewLifecycleOwner,
            binding
        )

        worker.setDistortions()
        worker.clearStructure()
        setTopAppBarActions()
        disableAllFields()
        showNote()
        return binding.root
    }

    private fun setTopAppBarActions() {
        val activity = activity as MainActivity
        activity.binding.apply {
            viewModel.selectedNote.observe(viewLifecycleOwner) { note ->
                noteTopBar.title = (note.date.trimStart('0') + ", " + note.time)

                noteTopBar.setNavigationOnClickListener {
                    activity.onBackPressed()
                }
                noteTopBar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.edit -> {
                            editNoteTopBar.isVisible = true
                            noteTopBar.isVisible = false
                            activity.showFragment(EditNoteFragment.newInstance(), R.id.edit)
                            viewModel.changeLoadMode(true)
                            true
                        }

                        R.id.delete -> {
                            worker.showDeleteDialog(note, layoutInflater.inflate(R.layout.delete_dialog, null))
                            true
                        }

                        else -> false
                    }
                }
            }
        }
    }

    private fun disableAllFields() {
        binding.apply {
            dateLayout.isVisible = false
            timeLayout.isVisible = false
            worker.boxes.forEach {
                it.isVisible = false
            }
            addEmotionButton.isVisible = false

            // На экране просмотра скрываем блок "Уровень дискомфорта после записи"
            levelAfter.isVisible = false
            levelAfterLayout.isVisible = false

            // Проценты рядом с полосками нам не нужны в режиме просмотра — оставляем только цветную полосу
            percentsBefore.isVisible = false
            percentsAfter.isVisible = false

            // Превращаем поля ввода в «просмотр» в стиле iOS: без рамок, только текст
            val layouts = listOf(
                situationLayout,
                thoughtsLayout,
                feelingsLayout,
                actionsLayout,
                answerLayout
            )
            layouts.forEach { layout ->
                layout.isHintEnabled = false
                layout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE
                layout.isEnabled = false
            }

            val fields = listOf(
                situationText,
                thoughtsText,
                feelingsText,
                actionsText,
                answerText
            )
            fields.forEach { editText ->
                editText.isEnabled = false
                editText.isFocusable = false
                editText.isCursorVisible = false
                editText.setBackgroundColor(Color.TRANSPARENT)
            }

            // Блокируем ползунки, чтобы выглядели как статичные индикаторы
            seekBarBefore.setOnTouchListener { _, _ -> true }
            seekBarAfter.setOnTouchListener { _, _ -> true }
        }
    }

    private fun showNote() {
        viewModel.selectedNote.observe(viewLifecycleOwner) {
            binding.apply {
                situationText.setText(it.situation)
                thoughtsText.setText(it.thoughts)

                worker.showFilledFields(it)

                showDistortions(it.distortions.split(";"))

                // Мягко подсветить фон экрана по уровню дискомфорта до записи
                val progress = it.discomfortBefore
                    .filter { ch -> ch.isDigit() }
                    .toIntOrNull()
                    ?.coerceIn(0, 100)
                    ?: 0
                val base = moodBaseColor(progress)
                val soft = Color.argb(
                    28,
                    Color.red(base),
                    Color.green(base),
                    Color.blue(base)
                )
                root.setBackgroundColor(soft)
            }
        }
        showEmotions()
    }

    private fun showEmotions() {
        viewModel.selectedNote.observe(viewLifecycleOwner) { note ->
            viewModel.getNoteEmotionsById(note.id).observe(viewLifecycleOwner) { emotions ->
                // Показываем эмоции одной непрерывной последовательностью в порядке из БД
                binding.selectedEmotions.removeAllViews()
                emotions.forEach {
                    val chip = Chip(context)
                    chip.setChipBackgroundColorResource(R.color.light_gray)
                    chip.setChipStrokeColorResource(
                        com.google.android.material.R.color.mtrl_btn_transparent_bg_color
                    )
                    chip.setTextAppearance(R.style.ChipTextAppearance)
                    chip.text = it.name
                    chip.isEnabled = false
                    binding.selectedEmotions.addView(chip)
                }
            }
        }
    }

    private fun showDistortions(distortions: List<String>) {
        binding.distortionsText.apply {
            text = distortions.map { "•   $it" }.joinToString(separator = "\n")
            setLineSpacing(1F, 1.5F)
        }
    }

    private fun moodBaseColor(progress: Int): Int = when {
        progress >= 67 -> Color.parseColor("#34C759") // green
        progress >= 34 -> Color.parseColor("#FF9500") // orange
        progress > 0 -> Color.parseColor("#FF3B30")   // red
        else -> Color.parseColor("#C7C7CC")           // neutral
    }

    companion object {
        @JvmStatic
        fun newInstance() = NoteFragment()
    }
}
