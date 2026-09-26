package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfDocumentHelper {

    data class PdfMeta(
        val pageCount: Int,
        val fileName: String,
        val fileSizeFormatted: String
    )

    suspend fun getPdfMeta(context: Context, uri: Uri): PdfMeta? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            var fileName = "Document.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val sizeStr = when {
                fileSize > 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", fileSize / (1024f * 1024f))
                fileSize > 1024 -> String.format(java.util.Locale.US, "%.1f KB", fileSize / 1024f)
                else -> "$fileSize B"
            }

            PdfMeta(
                pageCount = pageCount,
                fileName = fileName,
                fileSizeFormatted = sizeStr
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    suspend fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        maxDim: Int = 1800
    ): Bitmap? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            renderer = PdfRenderer(pfd)

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
            page = renderer.openPage(pageIndex)

            // Determine scale to achieve crisp clarity for Sindhi text up to maxDim
            val origWidth = page.width
            val origHeight = page.height

            val scale = min(maxDim.toFloat() / origWidth, maxDim.toFloat() / origHeight).coerceAtLeast(1.5f).coerceAtMost(3.0f)
            val renderWidth = (origWidth * scale).toInt()
            val renderHeight = (origHeight * scale).toInt()

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // PDF pages have transparent backgrounds by default, paint white

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }
}
