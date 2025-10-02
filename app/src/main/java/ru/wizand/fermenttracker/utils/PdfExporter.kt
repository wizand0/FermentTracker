package ru.wizand.fermenttracker.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.Stage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter(private val context: Context) {

    suspend fun exportBatchToPdf(
        batch: Batch,
        stages: List<Stage>,
        logs: List<BatchLog>,
        photos: List<Photo>,
        recipe: Recipe?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: throw IllegalStateException(context.getString(R.string.documents_directory_unavailable))

            val file = File(docsDir, "batch_${batch.name}_${System.currentTimeMillis()}.pdf")
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, com.itextpdf.kernel.geom.PageSize.A4)
            document.setMargins(30f, 30f, 30f, 30f)

            // Add all sections
            addTitle(document, batch)
            addProductInfo(document, batch, recipe)
            addWeightInfo(document, batch, logs)
            addStagesTable(document, stages)

            if (logs.any { it.weightGr != null }) {
                addWeightChart(document, logs)
            }

            if (photos.isNotEmpty()) {
                addPhotosSection(document, photos)
            }

            document.close()
            file
        } catch (e: Exception) {
            android.util.Log.e("PdfExporter", "Error exporting PDF", e)
            null
        }
    }

    fun savePdfToDownloads(file: File): Uri? {
        return try {
            val fileName = file.name

            // Для Android 10+ используем MediaStore API
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            uri
        } catch (e: Exception) {
            android.util.Log.e("PdfExporter", "Error saving PDF to Downloads", e)
            null
        }
    }

    // Вариант 1: Сохранить И поделиться
    fun saveAndSharePdf(file: File) {
        // Сначала сохраняем в Загрузки
        val savedUri = savePdfToDownloads(file)

        if (savedUri != null) {
            // Показываем уведомление об успешном сохранении
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.pdf_saved_to_downloads),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Затем предлагаем поделиться
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_pdf)))
    }

    // Вариант 2: Только сохранить (без диалога отправки)
    fun savePdfOnly(file: File): Boolean {
        val savedUri = savePdfToDownloads(file)
        return if (savedUri != null) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.pdf_saved_to_downloads),
                android.widget.Toast.LENGTH_LONG
            ).show()
            true
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.error_saving_pdf),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    // Оригинальный метод (только поделиться)
    fun sharePdf(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_pdf)))
    }

    // ========== PRIVATE HELPER METHODS ==========

    private fun addTitle(document: Document, batch: Batch) {
//        val titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
//        val dateFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val titleFont = getFontFromAssets(isBold = true)
        val dateFont = getFontFromAssets(isBold = false)

        val title = Paragraph(batch.name)
            .setFont(titleFont)
            .setFontSize(20f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5f)

        val subtitle = Paragraph(context.getString(R.string.creation_date_title, formatDate(batch.startDate)))
            .setFont(dateFont)
            .setFontSize(12f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)

        document.add(title)
        document.add(subtitle)
    }

    private fun addProductInfo(document: Document, batch: Batch, recipe: Recipe?) {
//        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
//        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val boldFont = getFontFromAssets(isBold = true)
        val regularFont = getFontFromAssets(isBold = false)

        document.add(Paragraph(context.getString(R.string.product_information))
            .setFont(boldFont)
            .setFontSize(14f)
            .setMarginBottom(10f))

        document.add(Paragraph(context.getString(R.string.product_type, batch.type))
            .setFont(regularFont)
            .setFontSize(11f))

        recipe?.let {
            // Ingredients
            if (!it.ingredients.isNullOrEmpty()) {
                document.add(Paragraph(context.getString(R.string.recipe_ingredients))
                    .setFont(boldFont)
                    .setFontSize(11f)
                    .setMarginTop(10f))

                it.ingredients.split("\n").forEach { ingredient ->
                    if (ingredient.isNotBlank()) {
                        document.add(Paragraph("• $ingredient")
                            .setFont(regularFont)
                            .setFontSize(10f)
                            .setMarginLeft(15f))
                    }
                }
            }

            // Preparation method
            if (!it.note.isNullOrEmpty()) {
                document.add(Paragraph(context.getString(R.string.cooking_technology))
                    .setFont(boldFont)
                    .setFontSize(11f)
                    .setMarginTop(10f))

                document.add(Paragraph(it.note)
                    .setFont(regularFont)
                    .setFontSize(10f)
                    .setMarginLeft(15f)
                    .setMarginBottom(15f))
            }
        }
    }

    private fun addWeightInfo(document: Document, batch: Batch, logs: List<BatchLog>) {
//        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
//        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val boldFont = getFontFromAssets(isBold = true)
        val regularFont = getFontFromAssets(isBold = false)

        document.add(Paragraph(context.getString(R.string.weight_info_title))
            .setFont(boldFont)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(10f))

        val initialWeight = batch.initialWeightGr ?: 0.0
        val currentWeight = batch.currentWeightGr ?: initialWeight
        val weightLoss = initialWeight - currentWeight
        val weightLossPercent = if (initialWeight > 0) (weightLoss / initialWeight * 100) else 0.0

        document.add(Paragraph(context.getString(R.string.initial_weight_text, initialWeight))
            .setFont(regularFont)
            .setFontSize(11f))

        document.add(Paragraph(context.getString(R.string.current_weight_text, currentWeight))
            .setFont(regularFont)
            .setFontSize(11f))

        document.add(Paragraph(context.getString(R.string.total_weight_loss_text, weightLoss, weightLossPercent))
            .setFont(regularFont)
            .setFontSize(11f)
            .setMarginBottom(15f))
    }

    private fun addStagesTable(document: Document, stages: List<Stage>) {
//        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
//        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val boldFont = getFontFromAssets(isBold = true)
        val regularFont = getFontFromAssets(isBold = false)

        document.add(Paragraph(context.getString(R.string.stages_production_title))
            .setFont(boldFont)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(10f))

        val table = Table(UnitValue.createPercentArray(floatArrayOf(15f, 12f, 18f, 18f, 18f, 19f)))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        // Header
        val headers = listOf(
            context.getString(R.string.stage_column_header),
            context.getString(R.string.duration_column_header),
            context.getString(R.string.planned_start_column_header),
            context.getString(R.string.planned_end_column_header),
            context.getString(R.string.actual_start_column_header),
            context.getString(R.string.actual_end_column_header)
        )
        headers.forEach { header ->
            table.addHeaderCell(Cell()
                .add(Paragraph(header).setFont(boldFont).setFontSize(9f))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5f))
        }

        // Rows
        stages.sortedBy { it.orderIndex }.forEach { stage ->
            table.addCell(Cell().add(Paragraph(stage.name).setFont(regularFont).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(formatDuration(stage.durationHours)).setFont(regularFont).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(stage.plannedStartTime?.let { formatDateTime(it) } ?: "—").setFont(regularFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(stage.plannedEndTime?.let { formatDateTime(it) } ?: "—").setFont(regularFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(stage.startTime?.let { formatDateTime(it) } ?: "—").setFont(regularFont).setFontSize(8f)))
            table.addCell(Cell().add(Paragraph(stage.endTime?.let { formatDateTime(it) } ?: "—").setFont(regularFont).setFontSize(8f)))
        }

        document.add(table)
    }

    private fun addWeightChart(document: Document, logs: List<BatchLog>) {
//        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

        val boldFont = getFontFromAssets(isBold = true)

        document.add(Paragraph(context.getString(R.string.weight_chart_title))
            .setFont(boldFont)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(10f))

        val chartBitmap = createWeightChartBitmap(logs)
        if (chartBitmap != null) {
            val stream = java.io.ByteArrayOutputStream()
            chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val imageData = ImageDataFactory.create(stream.toByteArray())
            val image = Image(imageData)
                .setWidth(500f)
                .setAutoScale(true)
                .setMarginBottom(15f)
            document.add(image)
        }
    }

    private fun addPhotosSection(document: Document, photos: List<Photo>) {
//        val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
//        val regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val boldFont = getFontFromAssets(isBold = true)
        val regularFont = getFontFromAssets(isBold = false)

        document.add(Paragraph(context.getString(R.string.photos_title))
            .setFont(boldFont)
            .setFontSize(14f)
            .setMarginTop(15f)
            .setMarginBottom(10f))

        val selectedPhotos = selectPhotosEvenly(photos, 6)
        val table = Table(UnitValue.createPercentArray(floatArrayOf(33.33f, 33.33f, 33.33f)))
            .useAllAvailableWidth()

        selectedPhotos.forEach { photo ->
            val file = File(photo.filePath)
            if (file.exists()) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    val thumbnail = createThumbnail(bitmap, 200, 200)

                    val stream = java.io.ByteArrayOutputStream()
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val imageData = ImageDataFactory.create(stream.toByteArray())

                    val cell = Cell()
                        .add(Image(imageData).setWidth(150f).setAutoScale(true))
                        .add(Paragraph(formatDateTime(photo.timestamp))
                            .setFont(regularFont)
                            .setFontSize(8f)
                            .setTextAlignment(TextAlignment.CENTER))
                        .setPadding(5f)

                    table.addCell(cell)
                } catch (e: Exception) {
                    android.util.Log.e("PdfExporter", "Error adding photo to PDF", e)
                }
            }
        }

        document.add(table)
    }

    private fun createWeightChartBitmap(logs: List<BatchLog>): Bitmap? {
        val weightLogs = logs.filter { it.weightGr != null }.sortedBy { it.timestamp }
        if (weightLogs.isEmpty()) return null

        val width = 800
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            strokeWidth = 2f
        }

        val margin = 60f
        val chartWidth = width - 2 * margin
        val chartHeight = height - 2 * margin

        val weights = weightLogs.mapNotNull { it.weightGr }
        val minWeight = weights.minOrNull() ?: 0.0
        val maxWeight = weights.maxOrNull() ?: 100.0
        val weightRange = maxWeight - minWeight
        val weightPadding = weightRange * 0.1

        val minTime = weightLogs.first().timestamp
        val maxTime = weightLogs.last().timestamp
        val timeRange = maxTime - minTime

        // Draw axes
        paint.color = android.graphics.Color.BLACK
        paint.strokeWidth = 2f
        canvas.drawLine(margin, height - margin, width - margin, height - margin, paint)
        canvas.drawLine(margin, margin, margin, height - margin, paint)

        // Draw grid and Y-axis labels
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 1f
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 20f
            color = android.graphics.Color.BLACK
        }

        for (i in 0..5) {
            val y = height - margin - (chartHeight * i / 5)
            canvas.drawLine(margin, y, width - margin, y, paint)

            val weight = minWeight - weightPadding + (weightRange + 2 * weightPadding) * i / 5
            val label = String.format("%.0f", weight)
            canvas.drawText(label, 10f, y + 5f, textPaint)
        }

        // Draw X-axis labels (dates)
        for (i in 0..4) {
            val x = margin + (chartWidth * i / 4)
            val time = minTime + (timeRange * i / 4)
            val label = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(time))
            canvas.drawText(label, x - 20f, height - margin + 30f, textPaint)
        }

        // Draw data line
        paint.color = android.graphics.Color.BLUE
        paint.strokeWidth = 3f
        paint.style = android.graphics.Paint.Style.STROKE

        val path = android.graphics.Path()
        weightLogs.forEachIndexed { index, log ->
            val x = margin + ((log.timestamp - minTime).toFloat() / timeRange * chartWidth)
            val weight = log.weightGr ?: return@forEachIndexed
            val y = height - margin - (((weight - (minWeight - weightPadding)).toFloat() / (weightRange + 2 * weightPadding).toFloat()) * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, paint)

        // Draw data points
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.RED
        weightLogs.forEach { log ->
            val x = margin + ((log.timestamp - minTime).toFloat() / timeRange * chartWidth)
            val weight = log.weightGr ?: return@forEach
            val y = height - margin - (((weight - (minWeight - weightPadding)).toFloat() / (weightRange + 2 * weightPadding).toFloat()) * chartHeight)
            canvas.drawCircle(x, y, 5f, paint)
        }

        // Add title
        val titlePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 28f
            color = android.graphics.Color.BLACK
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(context.getString(R.string.weight_change_title), width / 2f - 120f, 30f, titlePaint)

        return bitmap
    }

    private fun createThumbnail(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun selectPhotosEvenly(photos: List<Photo>, maxCount: Int): List<Photo> {
        if (photos.size <= maxCount) return photos

        val sorted = photos.sortedBy { it.timestamp }
        val step = photos.size.toFloat() / maxCount
        return (0 until maxCount).map { i ->
            sorted[(i * step).toInt()]
        }
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun formatDateTime(timestamp: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun formatDuration(hours: Long): String {
        val days = hours / 24
        val remainingHours = hours % 24
        return when {
            days > 0 && remainingHours > 0 -> context.getString(R.string.duration_days_hours, days, remainingHours)
            days > 0 -> context.getString(R.string.duration_days_only, days)
            else -> context.getString(R.string.duration_hours_only, remainingHours)
        }
    }

    private fun getFontFromAssets(isBold: Boolean): com.itextpdf.kernel.font.PdfFont {
        val fontName = if (isBold) "fonts/OpenSans-Bold.ttf" else "fonts/OpenSans.ttf"
        val inputStream = context.assets.open(fontName)
        val fontData = inputStream.readBytes()
        return PdfFontFactory.createFont(fontData, "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
    }
}