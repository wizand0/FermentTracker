package ru.wizand.fermenttracker.ui.batches

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchDetailBinding
import ru.wizand.fermenttracker.ui.adapters.LogsAdapter
import ru.wizand.fermenttracker.ui.adapters.StageAdapter
import ru.wizand.fermenttracker.vm.BatchDetailViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

class BatchDetailFragment : Fragment() {
    private var _binding: FragmentBatchDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BatchDetailFragmentArgs by navArgs()
    private val viewModel: BatchDetailViewModel by viewModels {
        BatchDetailViewModel.Factory(requireActivity().application, args.batchId)
    }
    private lateinit var stageAdapter: StageAdapter
    private lateinit var logsAdapter: LogsAdapter
    private var lastPhotoPath: String? = null
    private var lastWeight: Double? = null

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(null) else Toast.makeText(context, "Camera denied", Toast.LENGTH_SHORT).show()
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { saveAndAddPhoto(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupButtons()
    }

    private fun setupViews() {
        stageAdapter = StageAdapter()
        binding.rvStages.adapter = stageAdapter
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())

        logsAdapter = LogsAdapter()
        binding.rvLogs.adapter = logsAdapter
        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.tvBatchName.text = it.name
                binding.tvType.text = it.type
                binding.currentStageName.text = it.currentStage
                binding.tvBatchStartDate.text = formatDate(it.startDate)
            }
        }
        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            stageAdapter.submitList(stages)
            val currentStage = stages.find { it.endTime == null }
            currentStage?.let {
                val timeLeftMs = it.startTime?.let { start ->
                    start + TimeUnit.HOURS.toMillis(it.durationHours) - System.currentTimeMillis()
                } ?: 0L
                binding.timeLeft.text = formatTimeLeft(timeLeftMs)
                lastWeight = it.currentWeightGr
            }
        }
        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            lastPhotoPath = photos.firstOrNull()?.filePath
        }
        viewModel.logs.observe(viewLifecycleOwner) { logs ->
            logsAdapter.submitList(logs)
        }
    }

    private fun setupButtons() {
        binding.btnExportPdf.setOnClickListener {
            Toast.makeText(context, "Exporting to PDF", Toast.LENGTH_SHORT).show()
        }
        binding.btnSaveBatch.setOnClickListener {
            val updatedBatch = viewModel.batch.value?.copy(
                notes = binding.etNotes.text.toString(),
                qrCode = binding.etQrCode.text.toString()
            )
            updatedBatch?.let { viewModel.updateBatch(it) }
            Toast.makeText(context, "Batch saved", Toast.LENGTH_SHORT).show()
        }
//        binding.btnDeleteBatch.setOnClickListener {
//            viewModel.deleteBatch()
//            findNavController().popBackStack()
//        }
//        binding.btnAddStage.setOnClickListener {
//            val newStage = Stage(
//                id = UUID.randomUUID().toString(),
//                batchId = args.batchId,
//                name = "New Stage",
//                durationHours = 24,
//                orderIndex = (viewModel.stages.value?.size ?: 0)
//            )
//            viewModel.addStage(newStage)
//        }
        binding.btnAddPhoto.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            if (currentStage != null) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    takePicture.launch(null)
                } else {
                    requestCamera.launch(Manifest.permission.CAMERA)
                }
            }
        }
        binding.btnAddWeight.setOnClickListener {
            showWeightDialog()
        }
        binding.btnNextStage.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            currentStage?.let { completeStage(it) }
        }
        binding.btnRemoveStage.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            currentStage?.let {
                viewModel.deleteStage(it.id)
                Toast.makeText(context, "Stage removed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWeightDialog() {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(lastWeight?.toString() ?: "")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Add Weight")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val weight = input.text.toString().toDoubleOrNull()
                if (weight == null) {
                    Toast.makeText(context, "Invalid weight", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val currentStage = viewModel.stages.value?.find { it.endTime == null }
                currentStage?.let {
                    val updated = it.copy(currentWeightGr = weight)
                    viewModel.updateStage(updated)
                }
                val log = BatchLog(
                    id = UUID.randomUUID().toString(),
                    batchId = args.batchId,
                    timestamp = System.currentTimeMillis(),
                    weightGr = weight,
                    photoPath = lastPhotoPath
                )
                viewModel.addLog(log)
                Toast.makeText(context, "Weight added to log", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAndAddPhoto(bitmap: Bitmap) {
        val file = File(requireContext().filesDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        val currentStage = viewModel.stages.value?.find { it.endTime == null }
        val photo = Photo(
            stageId = currentStage?.id ?: return,
            filePath = file.absolutePath
        )
        viewModel.addPhoto(photo)
        val log = BatchLog(
            id = UUID.randomUUID().toString(),
            batchId = args.batchId,
            timestamp = System.currentTimeMillis(),
            weightGr = lastWeight,
            photoPath = file.absolutePath
        )
        viewModel.addLog(log)
        Toast.makeText(context, "Photo added to log", Toast.LENGTH_SHORT).show()
    }

    private fun completeStage(stage: Stage) {
        val updated = stage.copy(endTime = System.currentTimeMillis())
        viewModel.updateStage(updated)
        val stages = viewModel.stages.value ?: return
        val nextIndex = stage.orderIndex + 1
        val nextStage = stages.find { it.orderIndex == nextIndex }
        nextStage?.let {
            val started = it.copy(startTime = System.currentTimeMillis())
            viewModel.updateStage(started)
        }
        Toast.makeText(context, "Stage completed", Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(Date(timestamp))

    private fun formatTimeLeft(ms: Long): String {
        if (ms <= 0) return "Overdue"
        val days = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        return "$days d, $hours h left"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}