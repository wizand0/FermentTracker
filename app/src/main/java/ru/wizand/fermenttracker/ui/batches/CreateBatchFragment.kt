package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.databinding.FragmentCreateBatchBinding
import ru.wizand.fermenttracker.vm.BatchListViewModel

class CreateBatchFragment : Fragment() {

    private var _binding: FragmentCreateBatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()

    private val productTypes = listOf(
        "Cured meat", "Cured sausage", "Cheese", "Sauerkraut", "Kombucha", "Pickles",
        "Сыровяленое мясо", "Сыровяленая колбаса", "Домашний сыр", "Квашеная капуста", "Комбуча", "Солёные огурцы"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateBatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Spinner -> используем simple dropdown
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            productTypes
        )
        binding.spinnerType.adapter = adapter

        binding.etStartWeight.doAfterTextChanged {
            binding.tilStartWeight.error = null
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val type = binding.spinnerType.selectedItem?.toString() ?: productTypes.first()
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

            val batch = Batch(name = name, productType = type, startWeightGr = weightKg * 1000.0)
            viewModel.createBatch(batch)
            Toast.makeText(requireContext(), getString(R.string.batch_created), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}