package com.mawekk.sterdiary.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.NoteWorker
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.databinding.FragmentNewNoteBinding
import com.mawekk.sterdiary.db.DiaryViewModel

class EditNoteFragment : Fragment() {
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

        worker.setActions()
        worker.showSelectedEmotions(context)
        loadNote()
        viewModel.loadMode.observe(viewLifecycleOwner) {
            selectEmotionsAndDistortions(it)
            setTopAppBarActions(it)
        }
        return binding.root
    }

    private fun setTopAppBarActions(isLoadMode: Boolean) {
        val activity = activity as MainActivity
        activity.binding.editNoteTopBar.apply {
            setNavigationOnClickListener {
                worker.showBackDialog(layoutInflater.inflate(R.layout.back_dialog, null))
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.save_item -> {
                        selectEmotionsAndDistortions(isLoadMode)
                        val emotions = viewModel.selectedEmotions.value ?: emptyList()
                        val note =
                            viewModel.assembleNote(
                                binding,
                                viewModel.selectedNote.value!!.id,
                                worker.checkSelectedDistortions().joinToString(separator = ";")
                            )
                        if (viewModel.updateNote(note, emotions)) {
                            viewModel.selectNote(note)
                            viewModel.changeLoadMode(false)
                            activity.onBackPressed()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                R.string.all_fields_must_be_filled,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
                true
            }
        }
    }

    private fun loadNote() {
        viewModel.selectedNote.observe(viewLifecycleOwner) {
            binding.apply {
                dateText.setText(it.date)
                timeText.setText(it.time)
                situationText.setText(it.situation)
                thoughtsText.setText(it.thoughts)

                worker.showFilledFields(it)
            }
        }
    }

    private fun selectEmotionsAndDistortions(isLoadMode: Boolean) {
        if (isLoadMode) {
            viewModel.selectedNote.observe(viewLifecycleOwner) { note ->
                note.distortions.split(";").forEach { text ->
                    worker.boxes.find { it.text.toString() == text }?.isChecked = true
                }
                viewModel.getNoteEmotionsById(note.id).observe(viewLifecycleOwner) {
                    viewModel.selectEmotions(it)
                }
            }
            viewModel.changeLoadMode(false)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = EditNoteFragment()
    }
}
