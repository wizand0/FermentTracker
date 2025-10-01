package ru.wizand.fermenttracker.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LabelGenerator(private val context: Context) {

    fun generateLabel(batch: Batch, stages: List<Stage>): Bitmap {
        val width = 400
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        val calculatedHeight = calculateLabelHeight(batch, stages, width, density, scaledDensity)
        val height = calculatedHeight + (20f * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)

        // Draw border
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRect(4f, 4f, width - 4f, height - 4f, borderPaint)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        var y = 25f * density
        val centerX = width / 2f
        val sidePadding = 15f * density
        val maxTextWidth = width - (sidePadding * 2)
        val sectionSpacing = 5f * density

        // Draw title
        var titleSize = 20f * scaledDensity
        textPaint.textSize = titleSize
        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD

        var titleLines = wrapText(batch.name, textPaint, maxTextWidth)
        if (titleLines.size > 2 || titleLines.any { textPaint.measureText(it) > maxTextWidth }) {
            titleSize = 16f * scaledDensity
            textPaint.textSize = titleSize
            titleLines = wrapText(batch.name, textPaint, maxTextWidth)
        }

        val fmTitle = textPaint.fontMetrics
        val lineHeightTitle = Math.ceil((fmTitle.descent - fmTitle.ascent).toDouble()).toFloat() + fmTitle.leading
        titleLines.forEach { line ->
            canvas.drawText(line, centerX, y, textPaint)
            y += lineHeightTitle * 0.9f
        }
        y += sectionSpacing * 0.6f

        // Draw type
        textPaint.textSize = 12f * scaledDensity
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        val fmType = textPaint.fontMetrics
        val lineHeightType = Math.ceil((fmType.descent - fmType.ascent).toDouble()).toFloat() + fmType.leading
        val typeLines = wrapText(batch.type, textPaint, maxTextWidth)
        typeLines.forEach { line ->
            canvas.drawText(line, centerX, y, textPaint)
            y += lineHeightType * 0.95f
        }
        y += sectionSpacing * 0.7f

        // Draw initial weight
        textPaint.textSize = 10f * scaledDensity
        val fmWeight = textPaint.fontMetrics
        val lineHeightWeight = Math.ceil((fmWeight.descent - fmWeight.ascent).toDouble()).toFloat() + fmWeight.leading
        val weightText = context.getString(R.string.initial_weight, batch.initialWeightGr ?: 0.0)
        val weightLines = wrapText(weightText, textPaint, maxTextWidth)
        weightLines.forEach { line ->
            canvas.drawText(line, centerX, y, textPaint)
            y += lineHeightWeight * 0.95f
        }
        y += sectionSpacing * 0.7f

        // Draw first stage dates
        val fmDates = textPaint.fontMetrics
        val lineHeightDates = Math.ceil((fmDates.descent - fmDates.ascent).toDouble()).toFloat() + fmDates.leading
        val firstStage = stages.firstOrNull()
        if (firstStage != null) {
            val startDate = firstStage.plannedStartTime ?: batch.startDate
            val startText = context.getString(R.string.start_date, formatDateTimeLabel(startDate))
            val startLines = wrapText(startText, textPaint, maxTextWidth)
            startLines.forEach { line ->
                canvas.drawText(line, centerX, y, textPaint)
                y += lineHeightDates * 0.95f
            }
            y += 4f * density

            val endDate = firstStage.plannedEndTime ?: (startDate + TimeUnit.HOURS.toMillis(firstStage.durationHours))
            val endText = context.getString(R.string.expected_end, formatDateTimeLabel(endDate))
            val endLines = wrapText(endText, textPaint, maxTextWidth)
            endLines.forEach { line ->
                canvas.drawText(line, centerX, y, textPaint)
                y += lineHeightDates * 0.95f
            }
            y += sectionSpacing * 1.0f
        }

        // Draw stages list
        textPaint.textSize = 9f * scaledDensity
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        val fmStages = textPaint.fontMetrics
        val lineHeightStages = Math.ceil((fmStages.descent - fmStages.ascent).toDouble()).toFloat() + fmStages.leading
        val leftMargin = sidePadding

        stages.forEachIndexed { index, stage ->
            val stageNum = index + 1
            val stageName = "$stageNum. ${stage.name}"

            val nameLines = wrapText(stageName, textPaint, maxTextWidth)
            nameLines.forEach { line ->
                canvas.drawText(line, leftMargin, y, textPaint)
                y += lineHeightStages * 0.9f
            }

            val startTime = stage.plannedStartTime
            val endTime = stage.plannedEndTime
            if (startTime != null && endTime != null) {
                val dateStart = formatDateTimeLabel(startTime)
                val dateEnd = formatDateTimeLabel(endTime)
                val dateText1 = "  $dateStart -"
                val dateText2 = "  $dateEnd"

                canvas.drawText(dateText1, leftMargin, y, textPaint)
                y += lineHeightStages * 0.9f

                canvas.drawText(dateText2, leftMargin, y, textPaint)
                y += lineHeightStages * 0.9f
            }

            y += 3f * density
        }

        return bitmap
    }

    suspend fun saveLabelToGallery(bitmap: Bitmap, batchName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "label_${batchName}_${System.currentTimeMillis()}.png"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FermentTracker")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    return@withContext true
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val fermentDir = File(picturesDir, "FermentTracker")
                if (!fermentDir.exists()) {
                    fermentDir.mkdirs()
                }

                val file = File(fermentDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(file)
                context.sendBroadcast(intent)

                return@withContext true
            }

            false
        } catch (e: Exception) {
            android.util.Log.e("LabelGenerator", "Error saving to gallery", e)
            false
        }
    }

    suspend fun saveLabelToCache(bitmap: Bitmap, batchName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()

            val file = File(cachePath, "label_${batchName}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.util.Log.e("LabelGenerator", "Error saving to cache", e)
            null
        }
    }

    fun generateQRCode(text: String, size: Int): Bitmap {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    }

    fun createErrorBitmap(): Bitmap {
        val width = 400
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText(context.getString(R.string.no_batch_data), width / 2f, height / 2f, paint)

        return bitmap
    }

    // ========== PRIVATE HELPER METHODS ==========

    private fun calculateLabelHeight(
        batch: Batch,
        stages: List<Stage>,
        width: Int,
        density: Float,
        scaledDensity: Float
    ): Int {
        val sidePadding = 15f * density
        val maxTextWidth = width - (sidePadding * 2)
        val sectionSpacing = 5f * density

        var totalHeight = 25f * density

        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
        }

        // Title
        var titleSize = 20f * scaledDensity
        textPaint.textSize = titleSize
        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        var titleLines = wrapText(batch.name, textPaint, maxTextWidth)
        if (titleLines.size > 2 || titleLines.any { textPaint.measureText(it) > maxTextWidth }) {
            titleSize = 16f * scaledDensity
            textPaint.textSize = titleSize
            titleLines = wrapText(batch.name, textPaint, maxTextWidth)
        }
        val fmTitle = textPaint.fontMetrics
        val lineHeightTitle = Math.ceil((fmTitle.descent - fmTitle.ascent).toDouble()).toFloat() + fmTitle.leading
        totalHeight += titleLines.size * lineHeightTitle * 0.9f + sectionSpacing * 0.6f

        // Type
        textPaint.textSize = 12f * scaledDensity
        textPaint.typeface = android.graphics.Typeface.DEFAULT
        val fmType = textPaint.fontMetrics
        val lineHeightType = Math.ceil((fmType.descent - fmType.ascent).toDouble()).toFloat() + fmType.leading
        val typeLines = wrapText(batch.type, textPaint, maxTextWidth)
        totalHeight += typeLines.size * lineHeightType * 0.95f + sectionSpacing * 0.7f

        // Weight
        textPaint.textSize = 10f * scaledDensity
        val fmWeight = textPaint.fontMetrics
        val lineHeightWeight = Math.ceil((fmWeight.descent - fmWeight.ascent).toDouble()).toFloat() + fmWeight.leading
        val weightText = context.getString(R.string.initial_weight, batch.initialWeightGr ?: 0.0)
        val weightLines = wrapText(weightText, textPaint, maxTextWidth)
        totalHeight += weightLines.size * lineHeightWeight * 0.95f + sectionSpacing * 0.7f

        // Dates
        val fmDates = textPaint.fontMetrics
        val lineHeightDates = Math.ceil((fmDates.descent - fmDates.ascent).toDouble()).toFloat() + fmDates.leading
        val firstStage = stages.firstOrNull()
        if (firstStage != null) {
            val startDate = firstStage.plannedStartTime ?: batch.startDate
            val startText = context.getString(R.string.start_date, formatDateTimeLabel(startDate))
            val startLines = wrapText(startText, textPaint, maxTextWidth)
            totalHeight += startLines.size * lineHeightDates * 0.95f + 4f * density

            val endDate = firstStage.plannedEndTime ?: (startDate + TimeUnit.HOURS.toMillis(firstStage.durationHours))
            val endText = context.getString(R.string.expected_end, formatDateTimeLabel(endDate))
            val endLines = wrapText(endText, textPaint, maxTextWidth)
            totalHeight += endLines.size * lineHeightDates * 0.95f + sectionSpacing * 1.0f
        }

        // Stages
        textPaint.textSize = 9f * scaledDensity
        val fmStages = textPaint.fontMetrics
        val lineHeightStages = Math.ceil((fmStages.descent - fmStages.ascent).toDouble()).toFloat() + fmStages.leading

        stages.forEach { stage ->
            val stageName = "${stages.indexOf(stage) + 1}. ${stage.name}"
            val nameLines = wrapText(stageName, textPaint, maxTextWidth)
            totalHeight += nameLines.size * lineHeightStages * 0.9f

            if (stage.plannedStartTime != null && stage.plannedEndTime != null) {
                totalHeight += 2 * lineHeightStages * 0.9f
            }

            totalHeight += 3f * density
        }

        return totalHeight.toInt()
    }

    private fun wrapText(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) {
            return listOf(text)
        }

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    val chars = word.toCharArray()
                    var charLine = ""
                    chars.forEach { char ->
                        val testCharLine = charLine + char
                        if (paint.measureText(testCharLine) > maxWidth && charLine.isNotEmpty()) {
                            lines.add(charLine)
                            charLine = char.toString()
                        } else {
                            charLine = testCharLine
                        }
                    }
                    if (charLine.isNotEmpty()) {
                        currentLine = charLine
                    }
                }
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun formatDateTimeLabel(timestamp: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}