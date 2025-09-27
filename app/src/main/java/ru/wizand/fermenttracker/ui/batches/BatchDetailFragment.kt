package ru.wizand.fermenttracker.ui.batches

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchDetailBinding
import ru.wizand.fermenttracker.ui.adapters.LogsAdapter
import ru.wizand.fermenttracker.ui.adapters.StageAdapter
import ru.wizand.fermenttracker.vm.BatchDetailViewModel
import ru.wizand.fermenttracker.vm.BatchListViewModel
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

    // activity-scoped viewmodel for global state & weight validation
    private val batchListViewModel: BatchListViewModel by activityViewModels()

    private lateinit var stageAdapter: StageAdapter
    private lateinit var logsAdapter: LogsAdapter

    private var lastPhotoPath: String? = null
    private var lastWeight: Double? = null

    // Activity result contracts
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(null) else Toast.makeText(context, "Camera denied", Toast.LENGTH_SHORT).show()
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { saveAndAddPhoto(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
        setupButtons()

        // init active stage in BatchListViewModel
        batchListViewModel.refreshActiveStage(args.batchId)
    }

    private fun setupViews() {
        stageAdapter = StageAdapter(
            onStartClicked = { stage -> startStageManual(stage) },
            onCompleteClicked = { stage -> completeStageAndMaybeStartNext(stage) },
            onDurationChanged = { updatedStage ->
                viewModel.updateStage(updatedStage)
                updateBatchPlannedCompletion()
            }
        )
        binding.rvStages.adapter = stageAdapter
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())

        logsAdapter = LogsAdapter()
        binding.rvLogs.adapter = logsAdapter
        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        // Active stage id from batchListViewModel controls adapter buttons visibility
        batchListViewModel.activeStageId.observe(viewLifecycleOwner) { activeId ->
            stageAdapter.setActiveStage(activeId)
        }

        // Observe result of weight saves (validation/errors)
        batchListViewModel.weightSaveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BatchListViewModel.WeightSaveResult.Success -> {
                    Toast.makeText(requireContext(), "Вес успешно сохранён", Toast.LENGTH_SHORT).show()
                    lastWeight = null // refresh from DB via batch observer
                }
                is BatchListViewModel.WeightSaveResult.Failure -> {
                    Toast.makeText(requireContext(), "Ошибка: ${result.reason}", Toast.LENGTH_LONG).show()
                }
                else -> { /* null */ }
            }
        }

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.tvBatchName.text = it.name
                binding.tvType.text = it.type
                binding.currentStageName.text = it.currentStage
                binding.tvBatchStartDate.text = formatDate(it.startDate)
                binding.tvInitialWeight.text = it.initialWeightGr?.let { w -> "Initial Weight: $w g" } ?: "Initial Weight: N/A"
                binding.etNotes.setText(it.notes ?: "")
                binding.etQrCode.setText(it.qrCode ?: "")

                // update adapter weight display
                stageAdapter.updateBatchWeight(it.currentWeightGr)

                // Prepare lastWeight from DB snapshot (current or initial)
                lastWeight = it.currentWeightGr ?: it.initialWeightGr
            }
        }

        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            val sorted = stages.sortedBy { it.orderIndex }
            stageAdapter.submitList(sorted)

            // determine active stage for header/time left
            val activeId = batchListViewModel.activeStageId.value
            val activeStage = sorted.find { it.id == activeId } ?: sorted.find { it.startTime != null && it.endTime == null }

            if (activeStage != null) {
                val timeLeftMs = (activeStage.startTime ?: System.currentTimeMillis()) + TimeUnit.HOURS.toMillis(activeStage.durationHours) - System.currentTimeMillis()
                binding.timeLeft.text = formatTimeLeft(timeLeftMs)
                binding.currentStageName.text = activeStage.name
            } else {
                val next = sorted.find { it.startTime == null && it.endTime == null }
                if (next != null) {
                    binding.timeLeft.text = "Not started"
                    binding.currentStageName.text = next.name
                } else {
                    binding.timeLeft.text = "N/A"
                }
            }
            stageAdapter.notifyDataSetChanged()
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
            // we write into app-specific external dir -> no WRITE_EXTERNAL_STORAGE needed
            exportToPdf()
        }

        binding.btnSaveBatch.setOnClickListener {
            val updated = viewModel.batch.value?.copy(
                notes = binding.etNotes.text.toString(),
                qrCode = binding.etQrCode.text.toString()
            )
            updated?.let { viewModel.updateBatch(it) }
            Toast.makeText(context, "Batch saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddStage.setOnClickListener {
            val newStage = Stage(
                id = UUID.randomUUID().toString(),
                batchId = args.batchId,
                name = "New Stage",
                durationHours = 24,
                orderIndex = (viewModel.stages.value?.size ?: 0),
                plannedStartTime = System.currentTimeMillis(),
                plannedEndTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
            )
            viewModel.addStage(newStage)
            updateBatchPlannedCompletion()
        }

        binding.btnAddPhoto.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            if (currentStage != null) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    takePicture.launch(null)
                } else {
                    requestCamera.launch(Manifest.permission.CAMERA)
                }
            } else {
                Toast.makeText(requireContext(), "Нет активного этапа для добавления фото", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddWeight.setOnClickListener {
            showWeightDialog()
        }
    }

    // Dialog to add weight — delegates validation + saving to BatchListViewModel
    private fun showWeightDialog() {
        val initial = viewModel.batch.value?.initialWeightGr
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText((lastWeight ?: initial)?.toString() ?: "")
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
                // Call VM — it will validate and save log + update batch current weight
                batchListViewModel.addWeightChecked(args.batchId, weight, lastPhotoPath)
                lastWeight = weight
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Save photo to internal storage (app external-files dir would also work), add Photo entity and log
    private fun saveAndAddPhoto(bitmap: Bitmap) {
        val file = File(requireContext().filesDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

        val currentStage = viewModel.stages.value?.find { it.endTime == null }
        val stageId = currentStage?.id
        if (stageId == null) {
            Toast.makeText(context, "Нет активного этапа для добавления фото", Toast.LENGTH_SHORT).show()
            return
        }

        val photo = Photo(stageId = stageId, filePath = file.absolutePath)
        viewModel.addPhoto(photo)

        val batch = viewModel.batch.value
        val weightToLog = lastWeight ?: batch?.currentWeightGr ?: batch?.initialWeightGr

        val log = BatchLog(
            id = UUID.randomUUID().toString(),
            batchId = args.batchId,
            timestamp = System.currentTimeMillis(),
            weightGr = weightToLog,
            photoPath = file.absolutePath
        )
        viewModel.addLog(log)
        lastPhotoPath = file.absolutePath
        lastWeight = weightToLog
        Toast.makeText(context, "Фото добавлено в журнал", Toast.LENGTH_SHORT).show()
    }

    // Start stage locally (update detail VM) and notify global VM
    private fun startStageManual(stage: Stage) {
        val now = System.currentTimeMillis()
        val updated = stage.copy(startTime = now, plannedEndTime = now + TimeUnit.HOURS.toMillis(stage.durationHours))
        viewModel.updateStage(updated)

        viewModel.batch.value?.let { batch ->
            val updatedBatch = batch.copy(currentStage = stage.name)
            viewModel.updateBatch(updatedBatch)
        }

        // notify global vm to set active stage
        batchListViewModel.startStageManual(args.batchId, stage.id, stage.durationHours, autoStopPrevious = false)
        Toast.makeText(requireContext(), "Stage start requested", Toast.LENGTH_SHORT).show()
    }

    // Complete stage locally and via global VM (may auto-start next)
    private fun completeStageAndMaybeStartNext(stage: Stage) {
        val now = System.currentTimeMillis()
        val finished = stage.copy(endTime = now)
        viewModel.updateStage(finished)

        // local next start for immediate UI responsiveness
        val stages = viewModel.stages.value ?: emptyList()
        val next = stages.find { it.orderIndex == stage.orderIndex + 1 }
        if (next != null) {
            val started = next.copy(startTime = now, plannedEndTime = now + TimeUnit.HOURS.toMillis(next.durationHours))
            viewModel.updateStage(started)
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = next.name)
                viewModel.updateBatch(updatedBatch)
            }
            // notify global vm to switch active
            batchListViewModel.completeStageAndMaybeStartNext(args.batchId, stage.id, stage.orderIndex, autoStartNext = true)
            Toast.makeText(requireContext(), "Stage completed — next started", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = "")
                viewModel.updateBatch(updatedBatch)
            }
            batchListViewModel.completeStageAndMaybeStartNext(args.batchId, stage.id, stage.orderIndex, autoStartNext = false)
            Toast.makeText(requireContext(), "Stage completed", Toast.LENGTH_SHORT).show()
        }
        updateBatchPlannedCompletion()
    }

    // Recalculate planned completion date based on stages
    private fun updateBatchPlannedCompletion() {
        val stages = viewModel.stages.value ?: return
        val totalMs = stages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours) }
        viewModel.batch.value?.let { batch ->
            val updated = batch.copy(plannedCompletionDate = batch.startDate + totalMs)
            viewModel.updateBatch(updated)
        }
    }

    // Export to PDF into app-specific external dir and share via FileProvider
    private fun exportToPdf() {
        val batch = viewModel.batch.value ?: return
        val stages = viewModel.stages.value ?: return

        // Use app-specific external documents folder (no runtime permission required)
        val docsDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (docsDir == null) {
            Toast.makeText(requireContext(), "Documents directory not available", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(docsDir, "batch_${batch.id}.pdf")
        try {
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph("Batch: ${batch.name}"))
            document.add(Paragraph("Type: ${batch.type}"))
            document.add(Paragraph("Start Date: ${formatDate(batch.startDate)}"))

            val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 20f, 20f, 20f))).useAllAvailableWidth()
            table.addHeaderCell("Stage")
            table.addHeaderCell("Duration (h)")
            table.addHeaderCell("Planned Start")
            table.addHeaderCell("Planned End")
            table.addHeaderCell("Weight (g)")

            stages.sortedBy { it.orderIndex }.forEach { stage ->
                table.addCell(stage.name)
                table.addCell(stage.durationHours.toString())
                table.addCell(stage.plannedStartTime?.let { formatDate(it) } ?: "N/A")
                table.addCell(stage.plannedEndTime?.let { formatDate(it) } ?: "N/A")
                table.addCell(viewModel.batch.value?.currentWeightGr?.toString() ?: viewModel.batch.value?.initialWeightGr?.toString() ?: "N/A")
            }

            document.add(table)
            document.close()

            // Share via FileProvider
            val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share PDF"))

            Toast.makeText(context, "PDF saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting PDF: ${e.message ?: e.toString()}", Toast.LENGTH_LONG).show()
        }
    }

    // Utility formatting
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
