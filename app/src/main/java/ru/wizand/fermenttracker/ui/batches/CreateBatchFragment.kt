package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentCreateBatchBinding
import ru.wizand.fermenttracker.vm.BatchListViewModel

class CreateBatchFragment : Fragment() {
    private var _binding: FragmentCreateBatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private var editedStages: List<Stage> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        setupButtons()
        setupFragmentResultListener()
    }

    private fun setupSpinner() {
        val types = resources.getStringArray(R.array.fermentation_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnLoadTemplate.setOnClickListener {
            val selectedType = binding.spinnerType.selectedItem.toString()
            val action = CreateBatchFragmentDirections.actionCreateBatchToBatchTemplate(selectedType)
            findNavController().navigate(action)
        }
        binding.btnCreateBatch.setOnClickListener {
            createBatch()
        }
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener("requestKey_stages", viewLifecycleOwner) { _, bundle ->
            editedStages = bundle.getParcelableArrayList("editedStages") ?: emptyList()
            Toast.makeText(context, "Stages loaded: ${editedStages.size}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createBatch() {
        val name = binding.etName.text.toString()
        val type = binding.spinnerType.selectedItem.toString()
        val notes = binding.etNotes?.text?.toString() ?: ""
        val qrCode = binding.etQrCode?.text?.toString()?.takeIf { it.isNotBlank() }
        if (name.isBlank()) {
            Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show()
            return
        }
        val batch = Batch(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            type = type,
            startDate = System.currentTimeMillis(),
            currentStage = editedStages.firstOrNull()?.name ?: "Подготовка",
            notes = notes,
            qrCode = qrCode
        )
        viewModel.createBatchWithStages(batch, editedStages)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}