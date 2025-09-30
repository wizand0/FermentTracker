package ru.wizand.fermenttracker.utils

import android.content.Context
import android.net.Uri
import java.io.*

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
            throw FileNotFoundException("Исходный файл не существует: ${sourceFile.path}")
        }
        if (!sourceFile.canRead()) {
            throw SecurityException("Нет доступа на чтение файла: ${sourceFile.path}")
        }
        if (sourceFile.length() == 0L) {
            throw IOException("Исходный файл пуст: ${sourceFile.path}")
        }

        try {
            FileInputStream(sourceFile).use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    val bytesCopied = copyStream(input, output)

                    // Проверяем, что скопировано корректное количество байт
                    if (bytesCopied != sourceFile.length()) {
                        throw IOException(
                            "Копирование неполное: ожидалось ${sourceFile.length()} байт, " +
                                    "скопировано $bytesCopied байт"
                        )
                    }
                } ?: throw IOException("Не удалось открыть поток записи для URI: $targetUri")
            }
        } catch (e: FileNotFoundException) {
            throw FileNotFoundException("Файл недоступен: ${e.message}")
        } catch (e: SecurityException) {
            throw SecurityException("Отказано в доступе: ${e.message}")
        } catch (e: IOException) {
            throw IOException("Ошибка при копировании файла: ${e.message}")
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
                throw IOException("Не удалось создать директорию: ${parentDir.path}")
            }
        }

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                // Проверяем, что поток не пуст
                if (input.available() == 0) {
                    throw IOException("Исходный поток пуст")
                }

                FileOutputStream(targetFile).use { output ->
                    val bytesCopied = copyStream(input, output)

                    // Проверяем, что что-то скопировано
                    if (bytesCopied == 0L) {
                        throw IOException("Не скопировано ни одного байта")
                    }
                }
            } ?: throw FileNotFoundException("Не удалось открыть поток чтения для URI: $sourceUri")
        } catch (e: FileNotFoundException) {
            // Удаляем частично скопированный файл
            targetFile.delete()
            throw FileNotFoundException("URI недоступен: ${e.message}")
        } catch (e: SecurityException) {
            targetFile.delete()
            throw SecurityException("Отказано в доступе к URI: ${e.message}")
        } catch (e: IOException) {
            targetFile.delete()
            throw IOException("Ошибка при копировании из URI: ${e.message}")
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
            throw IOException("Ошибка при копировании данных: ${e.message}")
        }

        return totalBytes
    }
}