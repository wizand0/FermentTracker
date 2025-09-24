package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import ru.wizand.fermenttracker.vm.BatchListViewModel
import androidx.fragment.app.activityViewModels

class BatchTemplateFragment : Fragment() {
    private var _binding: FragmentBatchTemplateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveTemplate.setOnClickListener {
            val name = binding.etTemplateName.text.toString()
            val stageCount = binding.etStageCount.text.toString().toIntOrNull() ?: 0
            val stagesText = binding.etStages.text.toString()

            if (name.isNotEmpty() && stageCount > 0) {
                // TODO: Implement template saving logic (e.g., save to database or ViewModel)
                Toast.makeText(context, "Template saved: $name", Toast.LENGTH_SHORT).show()
                // Example: viewModel.saveTemplate(name, stageCount, stagesText)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}