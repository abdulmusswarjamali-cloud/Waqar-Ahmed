package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object DocumentImageProcessor {

    fun getColorMatrix(filter: DocumentImageFilter): ColorMatrix {
        return when (filter) {
            DocumentImageFilter.ORIGINAL -> {
                ColorMatrix()
            }
            DocumentImageFilter.GRAYSCALE -> {
                ColorMatrix().apply { setSaturation(0f) }
            }
            DocumentImageFilter.HIGH_CONTRAST -> {
                val contrast = 1.65f
                val scale = contrast
                val translate = (-0.5f * contrast + 0.5f) * 255f
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            DocumentImageFilter.SHADOW_REMOVAL -> {
                // Boosts paper brightness, pushes off-white background to white, and neutralizes warm paper discoloration
                ColorMatrix(
                    floatArrayOf(
                        1.40f, 0f, 0f, 0f, 30f,
                        0f, 1.40f, 0f, 0f, 30f,
                        0f, 0f, 1.50f, 0f, 40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            DocumentImageFilter.MAGIC_BW -> {
                // High-slope luminance binarization: cleans document background to pure white while making text crisp black
                val slope = 4.2f
                val threshold = 138f
                val r = 0.299f * slope
                val g = 0.587f * slope
                val b = 0.114f * slope
                val offset = -threshold * slope + 128f
                ColorMatrix(
                    floatArrayOf(
                        r, g, b, 0f, offset,
                        r, g, b, 0f, offset,
                        r, g, b, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
    }

    suspend fun applyFilter(srcBitmap: Bitmap, filter: DocumentImageFilter): Bitmap = withContext(Dispatchers.Default) {
        if (filter == DocumentImageFilter.ORIGINAL) {
            return@withContext srcBitmap
        }

        val resultBitmap = Bitmap.createBitmap(
            srcBitmap.width,
            srcBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(getColorMatrix(filter))
        }
        canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
        resultBitmap
    }

    suspend fun rotateBitmap(srcBitmap: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        if (degrees % 360f == 0f) return@withContext srcBitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1800): Bitmap? = withContext(Dispatchers.IO) {
        try {
            var input: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            var inSampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            while ((maxOriginal / inSampleSize) > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            input = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()

            decodedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun cropBitmap(
        srcBitmap: Bitmap,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val l = (leftFraction.coerceIn(0f, 1f) * srcBitmap.width).toInt()
        val t = (topFraction.coerceIn(0f, 1f) * srcBitmap.height).toInt()
        val r = (rightFraction.coerceIn(0f, 1f) * srcBitmap.width).toInt()
        val b = (bottomFraction.coerceIn(0f, 1f) * srcBitmap.height).toInt()

        val width = (r - l).coerceAtLeast(10).coerceAtMost(srcBitmap.width - l)
        val height = (b - t).coerceAtLeast(10).coerceAtMost(srcBitmap.height - t)

        Bitmap.createBitmap(srcBitmap, l, t, width, height)
    }

    suspend fun prepareForFastOcr(srcBitmap: Bitmap, targetMaxDim: Int = 1280): Bitmap = withContext(Dispatchers.Default) {
        val maxDim = max(srcBitmap.width, srcBitmap.height)
        if (maxDim <= targetMaxDim) {
            return@withContext srcBitmap
        }
        val scale = targetMaxDim.toFloat() / maxDim
        val newWidth = (srcBitmap.width * scale).toInt()
        val newHeight = (srcBitmap.height * scale).toInt()
        Bitmap.createScaledBitmap(srcBitmap, newWidth, newHeight, true)
    }

    suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "scan_"): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "${prefix}${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file
    }
}
