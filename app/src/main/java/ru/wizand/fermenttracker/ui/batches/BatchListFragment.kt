package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentBatchListBinding
import ru.wizand.fermenttracker.ui.adapters.BatchAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel

class BatchListFragment : Fragment() {

    private var _binding: FragmentBatchListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BatchAdapter { batch ->
            val bundle = Bundle().apply {
                putString("batchId", batch.id)
            }
            findNavController().navigate(R.id.action_batchList_to_batchDetail, bundle)
        }
        binding.rvBatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBatches.adapter = adapter

        viewModel.batches.observe(viewLifecycleOwner) { batches ->
            adapter.submitList(batches)
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val batch = adapter.currentList[position]
                    viewModel.deleteBatch(batch)
                    Toast.makeText(context, "Batch ${batch.name} deleted", Toast.LENGTH_SHORT).show()
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvBatches)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}