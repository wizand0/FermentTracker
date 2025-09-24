package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentBatchDetailBinding
import ru.wizand.fermenttracker.ui.adapters.StageAdapter
import ru.wizand.fermenttracker.vm.BatchDetailViewModel


class BatchDetailFragment : Fragment() {
    private var _binding: FragmentBatchDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BatchDetailFragmentArgs by navArgs()
    private val viewModel: BatchDetailViewModel by viewModels { BatchDetailViewModel.Factory(requireActivity().application, args.batchId) }
    private lateinit var stageAdapter: StageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        stageAdapter = StageAdapter { stageId, action ->
            // handle: add weight / photo / next stage
            // For now stub
        }
        binding.rvStages.adapter = stageAdapter

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            if (batch != null) {
                binding.tvBatchName.text = batch.name
                binding.tvProductType.text = batch.productType
            }
        }
        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            stageAdapter.submitList(stages)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}