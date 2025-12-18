package com.mawekk.sterdiary.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import com.mawekk.sterdiary.RecyclerViewWorker
import com.mawekk.sterdiary.databinding.FragmentSearchBinding
import com.mawekk.sterdiary.db.DiaryViewModel

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: DiaryViewModel by activityViewModels()
    private lateinit var worker: RecyclerViewWorker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        worker = RecyclerViewWorker(
            binding.searchRecyclerView,
            requireActivity(),
            viewModel,
            viewLifecycleOwner
        )

        worker.initRecyclerView()
        worker.setRecyclerViewItemsForSearch()
        setSearchViewAction()
        return binding.root
    }

    private fun setSearchViewAction() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                worker.filterNotes(newText)
                return true
            }

        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}
