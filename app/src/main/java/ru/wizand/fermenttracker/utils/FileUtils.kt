package ru.wizand.fermenttracker.utils

import android.content.Context
import android.net.Uri
import java.io.*
import ru.wizand.fermenttracker.R

object FileUtils {

    /**
     * Копирует файл в URI с проверками безопасности
     * @throws FileNotFoundException если исходный файл не существует
     * @throws SecurityException если нет прав на чтение
     * @throws IOException при ошибках ввода-вывода
     */
    @Throws(IOException::class, FileNotFoundException::class, SecurityException::class)
    fun copyFileToUri(sourceFile: File, targetUri: Uri, context: Context) {
        // Проверки перед началом копирования
        if (!sourceFile.exists()) {
            throw FileNotFoundException(
                context.getString(
                    R.string.file_error_source_not_exists,
                    sourceFile.path
                )
            )
        }
        if (!sourceFile.canRead()) {
            throw SecurityException(
                context.getString(
                    R.string.file_error_no_read_access,
                    sourceFile.path
                )
            )
        }
        if (sourceFile.length() == 0L) {
            throw IOException(
                context.getString(
                    R.string.file_error_source_empty,
                    sourceFile.path
                )
            )
        }

        try {
            FileInputStream(sourceFile).use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    val bytesCopied = copyStream(input, output)

                    // Проверяем, что скопировано корректное количество байт
                    if (bytesCopied != sourceFile.length()) {
                        throw IOException(
                            context.getString(
                                R.string.file_error_copy_incomplete,
                                sourceFile.length(),
                                bytesCopied
                            )
                        )
                    }
                } ?: throw IOException(
                    context.getString(
                        R.string.file_error_cannot_open_output_stream,
                        targetUri.toString()
                    )
                )
            }
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException(
                context.getString(
                    R.string.file_error_file_unavailable,
                    e.message
                )
            )
        } catch (e: SecurityException) {
            throw SecurityException(
                context.getString(
                    R.string.file_error_access_denied,
                    e.message
                )
            )
        } catch (e: IOException) {
            throw IOException(
                context.getString(
                    R.string.file_error_copy_failed,
                    e.message
                )
            )
        }
    }

    /**
     * Копирует данные из URI в файл с проверками
     * @throws FileNotFoundException если URI недоступен
     * @throws IOException при ошибках ввода-вывода
     */
    @Throws(IOException::class, FileNotFoundException::class, SecurityException::class)
    fun copyUriToFile(sourceUri: Uri, targetFile: File, context: Context) {
        // Проверяем и создаем родительскую директорию
        val parentDir = targetFile.parentFile
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw IOException(
                    context.getString(
                        R.string.file_error_cannot_create_directory,
                        parentDir.path
                    )
                )
            }
        }

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                // Проверяем, что поток не пуст
                if (input.available() == 0) {
                    throw IOException(context.getString(R.string.file_error_source_stream_empty))
                }

                FileOutputStream(targetFile).use { output ->
                    val bytesCopied = copyStream(input, output)

                    // Проверяем, что что-то скопировано
                    if (bytesCopied == 0L) {
                        throw IOException(context.getString(R.string.file_error_no_bytes_copied))
                    }
                }
            } ?: throw FileNotFoundException(
                context.getString(
                    R.string.file_error_cannot_open_input_stream,
                    sourceUri.toString()
                )
            )
        } catch (e: FileNotFoundException) {
            // Удаляем частично скопированный файл
            targetFile.delete()
            throw FileNotFoundException(
                context.getString(
                    R.string.file_error_uri_unavailable,
                    e.message
                )
            )
        } catch (e: SecurityException) {
            targetFile.delete()
            throw SecurityException(
                context.getString(
                    R.string.file_error_access_denied_uri,
                    e.message
                )
            )
        } catch (e: IOException) {
            targetFile.delete()
            throw IOException(
                context.getString(
                    R.string.file_error_copy_from_uri_failed,
                    e.message
                )
            )
        }
    }

    /**
     * Копирует данные из одного потока в другой
     * @return количество скопированных байт
     */
    @Throws(IOException::class)
    private fun copyStream(input: InputStream, output: OutputStream): Long {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var totalBytes = 0L

        try {
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
            output.flush()
        } catch (e: IOException) {
            // Для этого исключения мы не можем использовать контекст,
            // так как copyStream не получает его. Но в текущем коде
            // этот catch не используется в вызовах copyStream,
            // поэтому ошибка не возникнет в нормальных условиях
            throw e
        }

        return totalBytes
    }
}