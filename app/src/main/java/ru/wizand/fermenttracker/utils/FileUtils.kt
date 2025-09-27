package ru.wizand.fermenttracker.utils

import android.content.Context
import android.net.Uri
import java.io.*

object FileUtils {

    @Throws(IOException::class)
    fun copyFileToUri(sourceFile: File, targetUri: Uri, context: Context) {
        FileInputStream(sourceFile).use { input ->
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                copyStream(input, output)
            } ?: throw IOException("Unable to open output stream for URI: $targetUri")
        }
    }

    @Throws(IOException::class)
    fun copyUriToFile(sourceUri: Uri, targetFile: File, context: Context) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                copyStream(input, output)
            }
        } ?: throw IOException("Unable to open input stream for URI: $sourceUri")
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (true) {
            bytesRead = input.read(buffer)
            if (bytesRead <= 0) break
            output.write(buffer, 0, bytesRead)
        }
        output.flush()
    }
}
