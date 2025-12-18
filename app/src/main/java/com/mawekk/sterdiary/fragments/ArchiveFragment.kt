package com.mawekk.sterdiary.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mawekk.sterdiary.MainActivity
import com.mawekk.sterdiary.RecyclerViewWorker
import com.mawekk.sterdiary.databinding.FragmentArchiveBinding
import com.mawekk.sterdiary.db.DiaryViewModel

class ArchiveFragment : Fragment() {
    private lateinit var binding: FragmentArchiveBinding
    private val viewModel: DiaryViewModel by activityViewModels()
    private lateinit var worker: RecyclerViewWorker
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentArchiveBinding.inflate(inflater, container, false)
        worker = RecyclerViewWorker(
            binding.noteRecyclerView,
            requireActivity(),
            viewModel,
            viewLifecycleOwner
        )

        worker.initRecyclerView()
        worker.setRecyclerViewItemsForArchive()

        binding.addNoteButton.setOnClickListener {
            (requireActivity() as? MainActivity)?.openNewNote()
        }
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = ArchiveFragment()
    }
}
