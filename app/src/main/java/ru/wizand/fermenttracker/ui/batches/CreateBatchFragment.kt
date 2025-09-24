package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentCreateBatchBinding
import ru.wizand.fermenttracker.vm.BatchListViewModel

class CreateBatchFragment : Fragment() {

    private var _binding: FragmentCreateBatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private var selectedStages: List<Stage> = emptyList() // Store selected template stages

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateBatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize productTypes


        val productTypes = listOf(
            getString(R.string.product_cured_meat),
            getString(R.string.product_cured_sausage),
            getString(R.string.product_cheese),
            getString(R.string.product_sauerkraut),
            getString(R.string.product_kombucha),
            getString(R.string.product_pickles)
        )

        // Set up spinner adapter
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            productTypes
        )
        binding.spinnerType.adapter = adapter

        // Define standard recipes (stages) for each product type
        val templateStages = RecipeTemplates.getTemplateStages(requireContext())

        // Click listener for btnTemplateStages
        binding.btnTemplateStages.setOnClickListener {
            val selectedType = binding.spinnerType.selectedItem as? String
            if (selectedType != null) {
                selectedStages = templateStages[selectedType] ?: emptyList()
                Toast.makeText(requireContext(), "Loaded ${selectedStages.size} stages for $selectedType", Toast.LENGTH_SHORT).show()
                // Optionally, display stages in UI (e.g., in a RecyclerView or TextView)
                // For now, store in selectedStages for saving with btnSave
            } else {
                Toast.makeText(requireContext(), "Select a product type first", Toast.LENGTH_SHORT).show()
            }
        }

        // Click listener for btnSave
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val type = binding.spinnerType.selectedItem as String
            val weightText = binding.etStartWeight.text.toString().trim()
            val weightKg = weightText.toDoubleOrNull()

            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.error_name_required)
                return@setOnClickListener
            }
            if (weightKg == null || weightKg <= 0.0) {
                binding.tilStartWeight.error = getString(R.string.error_invalid_weight)
                return@setOnClickListener
            }

            val batch = Batch(
                name = name,
                productType = type,
                startWeightGr = weightKg * 1000.0
            )

            // Observe batch creation status
            viewModel.batchCreationStatus.observe(viewLifecycleOwner, Observer { result ->
                result.onSuccess {
                    // Save stages if any
                    selectedStages.forEach { stage ->
                        viewModel.viewModelScope.launch {
                            viewModel.addStage(stage.copy(batchId = batch.id))
                        }
                    }
                    Toast.makeText(requireContext(), getString(R.string.batch_created), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Failed to save batch: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            })

            viewModel.createBatch(batch)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}