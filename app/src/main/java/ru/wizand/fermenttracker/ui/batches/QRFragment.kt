package ru.wizand.fermenttracker.ui.batches

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import ru.wizand.fermenttracker.databinding.FragmentQrBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

class QRFragment : Fragment() {

    private var _binding: FragmentQrBinding? = null
    private val binding get() = _binding!!
    private val args: QRFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val batchId = args.batchId
        val text = batchId ?: "no-id"
        val qrBitmap = createQRCodeBitmap(text, 800)
        binding.ivQr.setImageBitmap(qrBitmap)

        binding.btnShare.setOnClickListener { shareBitmap(qrBitmap, "qr_${text}.png") }
        binding.btnSave.setOnClickListener { saveBitmapToCache(qrBitmap, "qr_${text}.png") }
    }

    private fun createQRCodeBitmap(text: String, size: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) for (y in 0 until height) bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        return bmp
    }

    private fun saveBitmapToCache(bitmap: Bitmap, filename: String): Uri? {
        val cachePath = File(requireContext().cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, filename)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".fileprovider", file)
    }

    private fun shareBitmap(bitmap: Bitmap, filename: String) {
        val uri = saveBitmapToCache(bitmap, filename) ?: return
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(share, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}