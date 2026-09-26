package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TextExportHelper {

    fun copyToClipboard(context: Context, text: String, label: String = "Sindhi OCR Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "سنڌي عبارت ڪاپي ٿي وئي (Copied to Clipboard)", Toast.LENGTH_SHORT).show()
    }

    fun shareViaWhatsApp(context: Context, text: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            // WhatsApp is not installed; fall back to standard system share chooser
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(fallbackIntent, "Share Sindhi Text")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun shareOrSaveTxtFile(context: Context, text: String, baseTitle: String = "sindhi_doc"): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeName = baseTitle.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(20)
            val fileName = "${safeName}_${timestamp}.txt"
            val file = File(context.cacheDir, fileName)

            FileOutputStream(file).use { out ->
                // Write with UTF-8 byte order mark (BOM) or standard UTF-8 for perfect Sindhi Arabic rendering in text editors
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(text.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Sindhi Extracted Document - $baseTitle")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Save / Share as TXT")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "فائل تيار آهي (TXT file prepared)", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving TXT: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    data class TextStats(
        val wordCount: Int,
        val charCount: Int,
        val lineCount: Int
    )

    fun getStats(text: String): TextStats {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return TextStats(wordCount = 0, charCount = 0, lineCount = 0)
        }
        val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val lines = trimmed.lines().filter { it.isNotBlank() }
        return TextStats(
            wordCount = words.size,
            charCount = trimmed.length,
            lineCount = lines.size
        )
    }
}
