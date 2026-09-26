package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs

enum class CropRatio {
    FREE,
    DOCUMENT_A4,
    SQUARE_1_1
}

@Composable
fun DocumentCropDialog(
    bitmap: Bitmap,
    onApplyCrop: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var leftFraction by remember { mutableFloatStateOf(0.05f) }
    var topFraction by remember { mutableFloatStateOf(0.05f) }
    var rightFraction by remember { mutableFloatStateOf(0.95f) }
    var bottomFraction by remember { mutableFloatStateOf(0.95f) }
    var selectedRatio by remember { mutableStateOf(CropRatio.FREE) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("crop_cancel_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "Crop Document (دستاويز ڪٽيو)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        onClick = {
                            leftFraction = 0f
                            topFraction = 0f
                            rightFraction = 1f
                            bottomFraction = 1f
                        },
                        modifier = Modifier.testTag("crop_reset_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Crop", tint = Color.White)
                    }
                }

                // Interactive Crop Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerWidth = maxWidth.value
                        val containerHeight = maxHeight.value
                        val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val containerAspect = containerWidth / containerHeight

                        val (viewWidth, viewHeight) = if (imgAspect > containerAspect) {
                            containerWidth to (containerWidth / imgAspect)
                        } else {
                            (containerHeight * imgAspect) to containerHeight
                        }

                        var activeHandle by remember { mutableStateOf<String?>(null) }

                        Box(
                            modifier = Modifier
                                .size(viewWidth.dp, viewHeight.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val xFrac = (offset.x / size.width).coerceIn(0f, 1f)
                                            val yFrac = (offset.y / size.height).coerceIn(0f, 1f)

                                            val hitThreshold = 0.08f
                                            val nearLeft = abs(xFrac - leftFraction) < hitThreshold
                                            val nearRight = abs(xFrac - rightFraction) < hitThreshold
                                            val nearTop = abs(yFrac - topFraction) < hitThreshold
                                            val nearBottom = abs(yFrac - bottomFraction) < hitThreshold

                                            activeHandle = when {
                                                nearTop && nearLeft -> "TL"
                                                nearTop && nearRight -> "TR"
                                                nearBottom && nearLeft -> "BL"
                                                nearBottom && nearRight -> "BR"
                                                nearLeft -> "L"
                                                nearRight -> "R"
                                                nearTop -> "T"
                                                nearBottom -> "B"
                                                xFrac in leftFraction..rightFraction && yFrac in topFraction..bottomFraction -> "MOVE"
                                                else -> null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val dx = dragAmount.x / size.width
                                            val dy = dragAmount.y / size.height
                                            val minSpan = 0.15f

                                            when (activeHandle) {
                                                "TL" -> {
                                                    leftFraction = (leftFraction + dx).coerceIn(0f, rightFraction - minSpan)
                                                    topFraction = (topFraction + dy).coerceIn(0f, bottomFraction - minSpan)
                                                }
                                                "TR" -> {
                                                    rightFraction = (rightFraction + dx).coerceIn(leftFraction + minSpan, 1f)
                                                    topFraction = (topFraction + dy).coerceIn(0f, bottomFraction - minSpan)
                                                }
                                                "BL" -> {
                                                    leftFraction = (leftFraction + dx).coerceIn(0f, rightFraction - minSpan)
                                                    bottomFraction = (bottomFraction + dy).coerceIn(topFraction + minSpan, 1f)
                                                }
                                                "BR" -> {
                                                    rightFraction = (rightFraction + dx).coerceIn(leftFraction + minSpan, 1f)
                                                    bottomFraction = (bottomFraction + dy).coerceIn(topFraction + minSpan, 1f)
                                                }
                                                "L" -> leftFraction = (leftFraction + dx).coerceIn(0f, rightFraction - minSpan)
                                                "R" -> rightFraction = (rightFraction + dx).coerceIn(leftFraction + minSpan, 1f)
                                                "T" -> topFraction = (topFraction + dy).coerceIn(0f, bottomFraction - minSpan)
                                                "B" -> bottomFraction = (bottomFraction + dy).coerceIn(topFraction + minSpan, 1f)
                                                "MOVE" -> {
                                                    val widthSpan = rightFraction - leftFraction
                                                    val heightSpan = bottomFraction - topFraction
                                                    val newL = (leftFraction + dx).coerceIn(0f, 1f - widthSpan)
                                                    val newT = (topFraction + dy).coerceIn(0f, 1f - heightSpan)
                                                    leftFraction = newL
                                                    topFraction = newT
                                                    rightFraction = newL + widthSpan
                                                    bottomFraction = newT + heightSpan
                                                }
                                            }
                                        },
                                        onDragEnd = { activeHandle = null }
                                    )
                                }
                        ) {
                            val imgBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw base image
                                drawImage(
                                    image = imgBitmap,
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                )

                                val cropL = leftFraction * size.width
                                val cropT = topFraction * size.height
                                val cropR = rightFraction * size.width
                                val cropB = bottomFraction * size.height
                                val cropW = cropR - cropL
                                val cropH = cropB - cropT

                                // Dark overlay outside crop rect
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    topLeft = Offset(0f, 0f),
                                    size = Size(size.width, cropT)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    topLeft = Offset(0f, cropB),
                                    size = Size(size.width, size.height - cropB)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    topLeft = Offset(0f, cropT),
                                    size = Size(cropL, cropH)
                                )
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    topLeft = Offset(cropR, cropT),
                                    size = Size(size.width - cropR, cropH)
                                )

                                // Crop border
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(cropL, cropT),
                                    size = Size(cropW, cropH),
                                    style = Stroke(width = 3.dp.toPx())
                                )

                                // Viewfinder Grid lines (Rule of Thirds)
                                val oneThirdW = cropW / 3f
                                val oneThirdH = cropH / 3f
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(cropL + oneThirdW, cropT),
                                    end = Offset(cropL + oneThirdW, cropB),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(cropL + oneThirdW * 2, cropT),
                                    end = Offset(cropL + oneThirdW * 2, cropB),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(cropL, cropT + oneThirdH),
                                    end = Offset(cropR, cropT + oneThirdH),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.45f),
                                    start = Offset(cropL, cropT + oneThirdH * 2),
                                    end = Offset(cropR, cropT + oneThirdH * 2),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // Corner handles
                                val cornerRadius = 10.dp.toPx()
                                val handleColor = Color(0xFF64B5F6)
                                drawCircle(color = handleColor, radius = cornerRadius, center = Offset(cropL, cropT))
                                drawCircle(color = handleColor, radius = cornerRadius, center = Offset(cropR, cropT))
                                drawCircle(color = handleColor, radius = cornerRadius, center = Offset(cropL, cropB))
                                drawCircle(color = handleColor, radius = cornerRadius, center = Offset(cropR, cropB))
                            }
                        }
                    }
                }

                // Aspect Ratio Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = selectedRatio == CropRatio.FREE,
                        onClick = { selectedRatio = CropRatio.FREE },
                        label = { Text("Free Crop") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedRatio == CropRatio.DOCUMENT_A4,
                        onClick = {
                            selectedRatio = CropRatio.DOCUMENT_A4
                            // Adjust to ~1:1.414 aspect
                            val currentW = rightFraction - leftFraction
                            val targetH = (currentW * 1.414f).coerceAtMost(0.95f)
                            topFraction = 0.05f
                            bottomFraction = (topFraction + targetH).coerceAtMost(0.98f)
                        },
                        label = { Text("A4 Ratio") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedRatio == CropRatio.SQUARE_1_1,
                        onClick = {
                            selectedRatio = CropRatio.SQUARE_1_1
                            val currentW = rightFraction - leftFraction
                            bottomFraction = (topFraction + currentW).coerceAtMost(0.98f)
                        },
                        label = { Text("1:1 Square") }
                    )
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            onApplyCrop(leftFraction, topFraction, rightFraction, bottomFraction)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("apply_crop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Crop")
                    }
                }
            }
        }
    }
}
