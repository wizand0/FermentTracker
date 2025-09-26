package ru.wizand.fermenttracker.ui.batches

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
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

    private val requestStorage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) exportToPdf() else Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
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
        stageAdapter = StageAdapter(
            onStartClicked = { stage -> startStageManual(stage) },
            onCompleteClicked = { stage -> completeStageAndMaybeStartNext(stage) },
            onDurationChanged = { updatedStage ->
                viewModel.updateStage(updatedStage)
                updateBatchPlannedCompletion() // Added: recalculate plannedCompletionDate on duration change
            }
        )
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

                // 🔥 загружаем заметку и QR-код при открытии
                binding.etNotes.setText(it.notes ?: "")
                binding.etQrCode.setText(it.qrCode ?: "")
            }
        }

        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            // обновляем список в адаптере
            stageAdapter.submitList(stages.sortedBy { it.orderIndex })

            // Выбираем активный этап — тот, который запущен (startTime != null) и не завершён (endTime == null)
            val activeStage = stages.find { it.startTime != null && it.endTime == null }

            if (activeStage != null) {
                val timeLeftMs = activeStage.startTime!! + TimeUnit.HOURS.toMillis(activeStage.durationHours) - System.currentTimeMillis()
                binding.timeLeft.text = formatTimeLeft(timeLeftMs)
                lastWeight = viewModel.batch.value?.currentWeightGr // Changed: use batch.currentWeightGr
                // обновим отображение текущего этапа в заголовке
                binding.currentStageName.text = activeStage.name
            } else {
                // нет запущенного этапа — попробуем показать первый незапущенный как "Следующий"
                val nextNotStarted = stages.sortedBy { it.orderIndex }.find { it.startTime == null && it.endTime == null }
                if (nextNotStarted != null) {
                    binding.timeLeft.text = "Not started"
                    binding.currentStageName.text = nextNotStarted.name
                } else {
                    binding.timeLeft.text = "N/A"
                }
                lastWeight = null
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
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                exportToPdf()
            } else {
                requestStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        binding.btnSaveBatch.setOnClickListener {
            val updatedBatch = viewModel.batch.value?.copy(
                notes = binding.etNotes.text.toString(),
                qrCode = binding.etQrCode.text.toString()
            )
            updatedBatch?.let { viewModel.updateBatch(it) }
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
            updateBatchPlannedCompletion() // Added: update planned completion after adding stage
        }
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
        /* binding.btnNextStage.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            currentStage?.let { completeStage(it) }
        } */
        /* binding.btnRemoveStage.setOnClickListener {
            val currentStage = viewModel.stages.value?.find { it.endTime == null }
            currentStage?.let {
                viewModel.deleteStage(it.id)
                Toast.makeText(context, "Stage removed", Toast.LENGTH_SHORT).show()
            }
        } */
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
                // Changed: update batch.currentWeightGr instead of stage
                viewModel.batch.value?.let { batch ->
                    val updatedBatch = batch.copy(currentWeightGr = weight)
                    viewModel.updateBatch(updatedBatch)
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
        val stageId = currentStage?.id
        if (stageId == null) {
            Toast.makeText(context, "Нет активного этапа для добавления фото", Toast.LENGTH_SHORT).show()
            return
        }

        val photo = Photo(
            stageId = stageId,
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
        Toast.makeText(context, "Фото добавлено в журнал", Toast.LENGTH_SHORT).show()
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

    // Запустить этап вручную: установить startTime, скорректировать plannedEndTime, обновить batch.currentStage
    private fun startStageManual(stage: Stage) {
        val now = System.currentTimeMillis()
        val updated = stage.copy(
            startTime = now,
            // обновляем прогнозируемый конец, чтобы Time Left сразу считался корректно
            plannedEndTime = now + TimeUnit.HOURS.toMillis(stage.durationHours)
        )
        viewModel.updateStage(updated)

        // также обновим Batch.currentStage (чтобы в заголовке отобразилось имя)
        viewModel.batch.value?.let { batch ->
            val updatedBatch = batch.copy(currentStage = stage.name)
            viewModel.updateBatch(updatedBatch)
        }

        Toast.makeText(requireContext(), "Stage started", Toast.LENGTH_SHORT).show()
    }

    // Завершить этап; опционально автоматически запустить следующий этап
    private fun completeStageAndMaybeStartNext(stage: Stage) {
        val now = System.currentTimeMillis()
        val finished = stage.copy(endTime = now)
        viewModel.updateStage(finished)

        // попробуем взять список stages из viewModel и найти следующий по orderIndex
        val stages = viewModel.stages.value ?: emptyList()
        val nextIndex = stage.orderIndex + 1
        val nextStage = stages.find { it.orderIndex == nextIndex }

        if (nextStage != null) {
            // авто-старт следующего этапа — можно сделать опциональным через настройки; сейчас включим по умолчанию
            val started = nextStage.copy(
                startTime = now,
                plannedEndTime = now + TimeUnit.HOURS.toMillis(nextStage.durationHours)
            )
            viewModel.updateStage(started)

            // обновим Batch.currentStage
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = nextStage.name)
                viewModel.updateBatch(updatedBatch)
            }
            Toast.makeText(requireContext(), "Stage completed — next started", Toast.LENGTH_SHORT).show()
        } else {
            // нет следующего — просто обновим Batch.currentStage на пустую или "Done"
            viewModel.batch.value?.let { batch ->
                val updatedBatch = batch.copy(currentStage = "")
                viewModel.updateBatch(updatedBatch)
            }
            Toast.makeText(requireContext(), "Stage completed", Toast.LENGTH_SHORT).show()
        }
        updateBatchPlannedCompletion() // Added: update after stage changes
    }

    // Added: helper to recalculate and update batch.plannedCompletionDate based on stages
    private fun updateBatchPlannedCompletion() {
        val stages = viewModel.stages.value ?: return
        val totalDurationMs = stages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours) }
        viewModel.batch.value?.let { batch ->
            val updated = batch.copy(plannedCompletionDate = batch.startDate + totalDurationMs)
            viewModel.updateBatch(updated)
        }
    }

    private fun exportToPdf() {
        val batch = viewModel.batch.value ?: return
        val stages = viewModel.stages.value ?: return
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "batch_${batch.id}.pdf")
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
                table.addCell("N/A") // Changed: weight is batch-level, not per stage
            }

            document.add(table)
            document.close()
            Toast.makeText(context, "PDF saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
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