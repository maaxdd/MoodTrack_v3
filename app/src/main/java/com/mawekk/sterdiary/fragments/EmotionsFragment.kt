package com.mawekk.sterdiary.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.databinding.FragmentEmotionsBinding
import com.mawekk.sterdiary.db.entities.Emotion
import com.mawekk.sterdiary.db.DiaryViewModel

class EmotionsFragment : Fragment() {
    private lateinit var binding: FragmentEmotionsBinding
    private val viewModel: DiaryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmotionsBinding.inflate(inflater, container, false)
        viewModel.editMode.observe(viewLifecycleOwner) {
            showEmotions(it)
            setTopAppBarActions(it)
        }
        setNewEmotionButton()
        return binding.root
    }

    private fun setNewEmotionButton() {
        val activity = activity as MainActivity
        binding.apply {
            addNewEmotionButton.setOnClickListener {
                val builder = AlertDialog.Builder(activity)
                val dialogLayout = layoutInflater.inflate(R.layout.new_emotion, null)
                builder.setView(dialogLayout)
                builder.setTitle(R.string.add_emotion)

                val dialog = builder.create()
                val name = dialogLayout.findViewById<EditText>(R.id.emotionText)
                val saveButton = dialogLayout.findViewById<Button>(R.id.saveButton)
                val cancelButton = dialogLayout.findViewById<Button>(R.id.cancelButton)

                saveButton.setOnClickListener {
                    if (name.text.isNotEmpty())
                        viewModel.addEmotion(Emotion(name.text.toString()))
                    dialog.dismiss()
                }
                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.setOnShowListener {
                    val titleId = resources.getIdentifier("alertTitle", "id", "android")
                    val dialogTitle = dialog.findViewById<View>(titleId) as TextView
                    dialogTitle.setTextColor(ContextCompat.getColor(activity, R.color.dark_blue))
                }
                dialog.show()
            }
        }
    }

    private fun setTopAppBarActions(isEditMode: Boolean) {
        val activity = activity as MainActivity
        activity.binding.emotionsTopBar.apply {
            if (isEditMode) {
                setNavigationIcon(null)
            } else {
                setNavigationIcon(R.drawable.ic_close)
                setNavigationOnClickListener { activity.onBackPressed() }
            }
            setOnMenuItemClickListener {
                viewModel.selectEmotions(checkSelectedEmotions())
                viewModel.changeEditMode(false)
                viewModel.changeLoadMode(false)
                activity.onBackPressed()
                true
            }
        }
    }

    private fun showEmotions(isIconVisible: Boolean) {
        viewModel.getAllEmotions().observe(viewLifecycleOwner) { emotions ->
            binding.emotionsChipGroup.removeAllViews()
            emotions.forEach {
                val chip = Chip(context)
                chip.setChipStrokeColorResource(com.google.android.material.R.color.mtrl_btn_transparent_bg_color)
                chip.setTextAppearance(R.style.ChipTextAppearance)
                chip.text = it.name
                chip.isCloseIconVisible = isIconVisible

                if (isIconVisible) {
                    chip.setOnCloseIconClickListener { view ->
                        binding.emotionsChipGroup.removeView(view)
                        viewModel.deselectEmotion(it)
                        viewModel.deleteEmotion(it)
                    }
                } else {
                    if (viewModel.isEmotionSelected(it)) {
                        chip.isActivated = true
                    }
                    chip.setOnClickListener {
                        emotionOnClick(chip)
                    }
                }

                checkChip(chip)
                binding.emotionsChipGroup.addView(chip)
            }
        }
    }

    private fun checkChip(chip: Chip) {
        if (chip.isActivated) {
            chip.setChipBackgroundColorResource(R.color.green)
        } else {
            chip.setChipBackgroundColorResource(R.color.light_gray)
        }
    }

    private fun emotionOnClick(chip: Chip) {
        chip.isActivated = !chip.isActivated
        checkChip(chip)
    }

    private fun checkSelectedEmotions(): List<Emotion> {
        val emotions = mutableListOf<Emotion>()
        binding.emotionsChipGroup.forEach {
            val chip = it as Chip
            if (chip.isActivated) {
                emotions.add(Emotion(chip.text.toString()))
            }
        }
        return emotions
    }


    companion object {
        @JvmStatic
        fun newInstance() = EmotionsFragment()
    }
}
