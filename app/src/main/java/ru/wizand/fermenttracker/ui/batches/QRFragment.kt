package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentQrBinding
import ru.wizand.fermenttracker.vm.BatchListViewModel

class QRFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем NavController
        navController = Navigation.findNavController(view)

        setupQrScanner()
    }

    private fun setupQrScanner() {
        // Placeholder для тестирования
        handleQrResult("test_qr_code")
    }

    private fun handleQrResult(qrCode: String) {
        lifecycleScope.launch {
            try {
                val batch = viewModel.findBatchByQrCode(qrCode)
                if (batch != null) {
                    // Используем Bundle вместо сгенерированного Directions
                    val bundle = Bundle().apply {
                        putString("batchId", batch.id)
                    }
                    navController.navigate(R.id.action_qr_to_batchDetail, bundle)
                } else {
                    showNotFoundDialog(qrCode)
                }
            } catch (e: Exception) {
                showErrorMessage("Ошибка поиска: ${e.message}")
            }
        }
    }

    private fun showNotFoundDialog(qrCode: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Продукт не найден")
            .setMessage("QR-код $qrCode не найден в базе данных")
            .setPositiveButton("OK", null)
            .setNegativeButton("Создать новый") { _, _ ->
                navController.navigate(R.id.action_qr_to_createBatch)
            }
            .show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}