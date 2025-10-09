package ru.wizand.fermenttracker.ui.batches

import android.Manifest
import android.content.Intent
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchDetailBinding
import ru.wizand.fermenttracker.ui.adapters.LogsAdapter
import ru.wizand.fermenttracker.ui.adapters.StageAdapter
import ru.wizand.fermenttracker.utils.LabelGenerator
import ru.wizand.fermenttracker.utils.NotificationHelper
import ru.wizand.fermenttracker.utils.PdfExporter
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

    private val batchListViewModel: BatchListViewModel by activityViewModels()

    private lateinit var stageAdapter: StageAdapter
    private lateinit var logsAdapter: LogsAdapter
    private lateinit var pdfExporter: PdfExporter
    private lateinit var labelGenerator: LabelGenerator

    private var lastPhotoPath: String? = null
    private var lastWeight: Double? = null

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(null)
        else Toast.makeText(context, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
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

        pdfExporter = PdfExporter(requireContext())
        labelGenerator = LabelGenerator(requireContext())

        setupViews()
        setupObservers()
        setupButtons()

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
        batchListViewModel.activeStageId.observe(viewLifecycleOwner) { activeId ->
            stageAdapter.setActiveStage(activeId)
        }

        batchListViewModel.weightSaveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BatchListViewModel.WeightSaveResult.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.weight_saved_successfully), Toast.LENGTH_SHORT).show()
                    lastWeight = null
                }
                is BatchListViewModel.WeightSaveResult.Failure -> {
                    Toast.makeText(requireContext(), getString(R.string.error_prefix, result.reason), Toast.LENGTH_LONG).show()
                }
                else -> { }
            }
        }

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.tvBatchName.text = it.name
                binding.tvType.text = it.type
                binding.currentStageName.text = it.currentStage
                binding.tvBatchStartDate.text = formatDate(it.startDate)
                binding.tvInitialWeight.text = it.initialWeightGr?.let { w ->
                    getString(R.string.initial_weight, w)
                } ?: getString(R.string.initial_weight_na)
                binding.etNotes.setText(it.notes ?: "")
//                binding.etQrCode.setText(it.qrCode ?: "")

                stageAdapter.updateBatchWeight(it.currentWeightGr)
                lastWeight = it.currentWeightGr ?: it.initialWeightGr
            }
        }

        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            val sorted = stages.sortedBy { it.orderIndex }
            stageAdapter.submitList(sorted)

            val activeId = batchListViewModel.activeStageId.value
            val activeStage = sorted.find { it.id == activeId } ?: sorted.find { it.startTime != null && it.endTime == null }

            if (activeStage != null) {
                val timeLeftMs = (activeStage.startTime ?: System.currentTimeMillis()) + TimeUnit.HOURS.toMillis(activeStage.durationHours) - System.currentTimeMillis()
                binding.timeLeft.text = formatTimeLeft(timeLeftMs)
                binding.currentStageName.text = activeStage.name
            } else {
                val next = sorted.find { it.startTime == null && it.endTime == null }
                if (next != null) {
                    binding.timeLeft.text = getString(R.string.not_started)
                    binding.currentStageName.text = next.name
                } else {
                    binding.timeLeft.text = getString(R.string.na)
                }
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
            exportToPdf()
        }

        binding.btnSaveBatch.setOnClickListener {
            val updated = viewModel.batch.value?.copy(
                notes = binding.etNotes.text.toString(),
//                qrCode = binding.etQrCode.text.toString()
            )
            updated?.let { viewModel.updateBatch(it) }
            Toast.makeText(context, getString(R.string.batch_saved_success), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), getString(R.string.no_active_stage_for_photo), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddWeight.setOnClickListener {
            showWeightDialog()
        }

        binding.btnPrintLabel.setOnClickListener {
            generateAndShowLabel()
        }
    }

    private fun showWeightDialog() {
        val initial = viewModel.batch.value?.initialWeightGr
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText((lastWeight ?: initial)?.toString() ?: "")
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_weight))
            .setView(input)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val weight = input.text.toString().toDoubleOrNull()
                if (weight == null) {
                    Toast.makeText(context, getString(R.string.invalid_weight), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                batchListViewModel.addWeightChecked(args.batchId, weight, lastPhotoPath)
                lastWeight = weight
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

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
        Toast.makeText(context, getString(R.string.photo_added_to_log_success), Toast.LENGTH_SHORT).show()
    }

    private fun startStageManual(stage: Stage) {
        val now = System.currentTimeMillis()
        val updated = stage.copy(startTime = now, plannedEndTime = now + TimeUnit.HOURS.toMillis(stage.durationHours))
        viewModel.updateStage(updated)

        viewModel.batch.value?.let { batch ->
            val updatedBatch = batch.copy(currentStage = stage.name)
            viewModel.updateBatch(updatedBatch)
            viewModel.scheduleStageNotification(updated, batch)
        }

        batchListViewModel.startStageManual(args.batchId, stage.id, stage.durationHours, autoStopPrevious = false)
        Toast.makeText(requireContext(), getString(R.string.stage_started_notification_scheduled1), Toast.LENGTH_SHORT).show()
    }

    private fun completeStageAndMaybeStartNext(stage: Stage) {
        val now = System.currentTimeMillis()
        val finished = stage.copy(endTime = now)
        viewModel.updateStage(finished)

        NotificationHelper.cancelNotification(requireContext(), stage.id.hashCode())

        val stages = viewModel.stages.value ?: emptyList()
        val next = stages.find { it.orderIndex == stage.orderIndex + 1 }
        if (next != null) {
            val started = next.copy(startTime = now, plannedEndTime = now + TimeUnit.HOURS.toMillis(next.durationHours))
            viewModel.updateStage(started)
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = next.name)
                viewModel.updateBatch(updatedBatch)
                viewModel.scheduleStageNotification(started, batch)
            }
            batchListViewModel.completeStageAndMaybeStartNext(args.batchId, stage.id, stage.orderIndex, autoStartNext = true)
            Toast.makeText(requireContext(), getString(R.string.stage_completed_next_started1), Toast.LENGTH_SHORT).show()
        } else {
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = "")
                viewModel.updateBatch(updatedBatch)
            }
            batchListViewModel.completeStageAndMaybeStartNext(args.batchId, stage.id, stage.orderIndex, autoStartNext = false)
            Toast.makeText(requireContext(), getString(R.string.stage_completed), Toast.LENGTH_SHORT).show()
        }
        updateBatchPlannedCompletion()
    }

    private fun updateBatchPlannedCompletion() {
        val stages = viewModel.stages.value ?: return
        val totalMs = stages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours) }
        viewModel.batch.value?.let { batch ->
            val updated = batch.copy(plannedCompletionDate = batch.startDate + totalMs)
            viewModel.updateBatch(updated)
        }
    }

    private fun exportToPdf() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Проверяем состояние фрагмента
                if (!isAdded()) return@launch

                val batch = viewModel.batch.value
                val stages = viewModel.stages.value ?: emptyList()
                val logs = viewModel.logs.value ?: emptyList()
                val photos = viewModel.photos.value ?: emptyList()
                val recipe = batch?.let { viewModel.getRecipeForBatch(it.type) }

                if (batch == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.no_batch_data_error), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val file = pdfExporter.exportBatchToPdf(batch, stages, logs, photos, recipe)

                withContext(Dispatchers.Main) {
                    // Проверяем состояние фрагмента снова
                    if (!isAdded()) return@withContext

                    if (file != null) {
//                        pdfExporter.sharePdf(file)
                        pdfExporter.saveAndSharePdf(file)
                        Toast.makeText(requireContext(), getString(R.string.pdf_saved_success, file.name), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.pdf_export_error_message), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded()) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.pdf_export_error_detailed, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                        android.util.Log.e("BatchDetailFragment", "PDF export error", e)
                    }
                }
            }
        }
    }

    private fun generateAndShowLabel() {
        val batch = viewModel.batch.value
        val stages = viewModel.stages.value?.sortedBy { it.orderIndex } ?: emptyList()

        if (batch == null) {
            Toast.makeText(requireContext(), getString(R.string.no_batch_data), Toast.LENGTH_SHORT).show()
            return
        }

        val labelBitmap = labelGenerator.generateLabel(batch, stages)
        showLabelPreviewDialog(labelBitmap, batch)
    }

    private fun showLabelPreviewDialog(labelBitmap: Bitmap, batch: Batch) {
        val imageView = android.widget.ImageView(requireContext()).apply {
            setImageBitmap(labelBitmap)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_preview))
            .setView(imageView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                saveLabelToGallery(labelBitmap, batch)
            }
            .setNeutralButton(getString(R.string.share)) { _, _ ->
                shareLabelImage(labelBitmap, batch)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun saveLabelToGallery(bitmap: Bitmap, batch: Batch) {
        lifecycleScope.launch {
            try {
                val result = labelGenerator.saveLabelToGallery(bitmap, batch.name)
                withContext(Dispatchers.Main) {
                    if (result) {
                        Toast.makeText(requireContext(), getString(R.string.label_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.label_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareLabelImage(bitmap: Bitmap, batch: Batch) {
        lifecycleScope.launch {
            try {
                val uri = labelGenerator.saveLabelToCache(bitmap, batch.name)
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Label"))
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.label_share_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(Date(timestamp))

    private fun formatTimeLeft(ms: Long): String {
        if (ms <= 0) return getString(R.string.overdue)
        val days = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        return getString(R.string.time_left_format, days, hours)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Очищаем слушатели в адаптерах
        if (::stageAdapter.isInitialized) {
            stageAdapter.clearAllListeners()
        }

        // Отсоединяем адаптеры от RecyclerView
        binding.rvStages.adapter = null
        binding.rvLogs.adapter = null

        // Очищаем binding
        _binding = null
    }
}