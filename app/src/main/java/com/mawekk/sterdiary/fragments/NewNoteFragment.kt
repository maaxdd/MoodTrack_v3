package com.mawekk.sterdiary.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.NoteWorker
import com.mawekk.sterdiary.R
import com.mawekk.sterdiary.databinding.FragmentNewNoteBinding
import com.mawekk.sterdiary.db.DiaryViewModel


class NewNoteFragment : Fragment() {
    private lateinit var binding: FragmentNewNoteBinding
    private lateinit var worker: NoteWorker
    private val viewModel: DiaryViewModel by activityViewModels()
    private var startEmotions = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewNoteBinding.inflate(layoutInflater, container, false)
        worker = NoteWorker(
            requireActivity(),
            activity as MainActivity,
            viewModel,
            viewLifecycleOwner,
            binding
        )

        worker.setDateAndTime()
        worker.setActions()
        worker.showSelectedEmotions(context)

        setTopAppBarActions()

        if (!startEmotions) {
            viewModel.selectEmotions(emptyList())
            startEmotions = true
        }

        return binding.root
    }

    private fun setTopAppBarActions() {
        // Вся логика верхней панели (стрелка "назад" и крестик "закрыть")
        // на экране новой записи теперь настраивается в NoteWorker.hookTopBar().
        // Здесь ничего не переопределяем, чтобы не сбивать работу мастера.
    }

    companion object {
        @JvmStatic
        fun newInstance() = NewNoteFragment()
    }
}
