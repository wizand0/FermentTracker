package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentBatchListBinding
import ru.wizand.fermenttracker.ui.adapters.BatchAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel


class BatchListFragment : Fragment() {

    private var _binding: FragmentBatchListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BatchListViewModel by viewModels()
    private lateinit var adapter: BatchAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = BatchAdapter { batch ->
            // click -> открыть детали
            val action = BatchListFragmentDirections.actionBatchListToBatchDetail(batch.id)
            findNavController().navigate(action)
        }
        binding.rvBatches.adapter = adapter

        viewModel.batches.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // example: pull to refresh or filters can be added
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}