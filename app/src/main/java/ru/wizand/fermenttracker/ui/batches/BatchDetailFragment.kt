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
import androidx.navigation.fragment.navArgs
import ru.wizand.fermenttracker.databinding.FragmentBatchDetailBinding
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.ui.adapters.StageAdapter
import ru.wizand.fermenttracker.vm.BatchDetailViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import ru.wizand.fermenttracker.R

class BatchDetailFragment : Fragment() {
    private var _binding: FragmentBatchDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BatchDetailFragmentArgs by navArgs()
    private val viewModel: BatchDetailViewModel by viewModels { BatchDetailViewModel.Factory(requireActivity().application, args.batchId) }
    private lateinit var stageAdapter: StageAdapter

    // Camera launchers
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(null) else Toast.makeText(context, "Camera denied", Toast.LENGTH_SHORT).show()
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { saveAndAddPhoto(it) }
    }
    private var currentStageForPhoto: Stage? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBatchDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stageAdapter = StageAdapter(
            onAction = { stageId: String, action: String ->
                viewModel.stages.value?.find { it.id == stageId }?.let { stage ->
                    when (action) {
                        "add_weight" -> updateWeight(stage)
                        "add_photo" -> addPhoto(stage)
                        "next" -> completeStage(stage)
                    }
                }
            },
            photosByStage = emptyMap()
        )
        binding.rvStages.adapter = stageAdapter

        viewModel.batch.observe(viewLifecycleOwner) { batch ->
            batch?.let {
                binding.tvBatchName.text = it.name
                binding.tvProductType.text = it.productType
                binding.tvBatchStartDate.text = formatDate(it.startDate)
            }
        }

        viewModel.stages.observe(viewLifecycleOwner) { stages ->
            stageAdapter.submitList(stages.sortedBy { it.orderIndex })
            updateCurrentStageInfo(stages)

            val photosMap = mutableMapOf<String, List<Photo>>()
            stages.forEach { stage ->
                viewModel.getPhotosForStage(stage.id).observe(viewLifecycleOwner) { photos ->
                    photosMap[stage.id] = photos
                    stageAdapter.updatePhotos(photosMap.toMap())
                }
            }
        }

        binding.btnExportPdf.setOnClickListener {
            findNavController().navigate(R.id.action_batchDetail_to_qrFragment)
        }
    }

    private fun updateCurrentStageInfo(stages: List<Stage>) {
        val currentStage = stages.firstOrNull { it.startTime != null && it.endTime == null }
        currentStage?.let {
            binding.currentStageName.text = it.name
            val timeLeftMs = it.startTime?.let { start ->
                (it.durationHours * 3600000) - (System.currentTimeMillis() - start)
            } ?: 0L
            binding.timeLeft.text = formatTimeLeft(timeLeftMs)
        } ?: run {
            binding.currentStageName.text = "All stages complete"
            binding.timeLeft.text = "0"
        }
    }

    private fun updateWeight(stage: Stage) {
        val input = EditText(requireContext()).apply {
            hint = "Enter weight in grams"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Update Weight")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val weight = input.text.toString().toDoubleOrNull() ?: return@setPositiveButton Toast.makeText(context, "Invalid weight", Toast.LENGTH_SHORT).show()
                val updated = stage.copy(currentWeightGr = weight)
                viewModel.updateStage(updated)
                Toast.makeText(context, "Weight updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addPhoto(stage: Stage) {
        currentStageForPhoto = stage
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture.launch(null)
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun saveAndAddPhoto(bitmap: Bitmap) {
        val file = File(requireContext().filesDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        val photo = Photo(stageId = currentStageForPhoto?.id ?: return, filePath = file.absolutePath)
        viewModel.addPhoto(photo)
        Toast.makeText(context, "Photo added", Toast.LENGTH_SHORT).show()
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

    private fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

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