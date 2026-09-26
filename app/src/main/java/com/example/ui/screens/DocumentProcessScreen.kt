package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ApiKeySettingsDialog
import com.example.ui.components.DocumentCropDialog
import com.example.ui.components.FilterSelectorBar
import com.example.ui.components.SindhiDiacriticsBar
import com.example.ui.viewmodel.DocScanUiState
import com.example.ui.viewmodel.DocScanViewModel
import com.example.util.DocumentImageFilter
import com.example.util.TextExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentProcessScreen(
    viewModel: DocScanViewModel,
    uiState: DocScanUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sindhiFontSize by remember { mutableFloatStateOf(20f) }
    var showTitleDialog by remember { mutableStateOf(false) }

    // Intercept hardware or system back button
    BackHandler {
        viewModel.navigateToHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showTitleDialog = true }
                    ) {
                        Column {
                            Text(
                                text = uiState.currentDocTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isPdf) {
                                    Text(
                                        text = "PDF • Page ${uiState.currentPdfPage + 1}/${uiState.pdfPageCount} • ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Filter: ${uiState.activeFilter.titleEnglish}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Title",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateToHome() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                },
                actions = {
                    // Header Crop Button
                    IconButton(
                        onClick = { viewModel.toggleCropDialog(true) },
                        modifier = Modifier.testTag("crop_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Crop Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Header Save Button
                    IconButton(
                        onClick = {
                            viewModel.saveDocument { success ->
                                if (success) {
                                    Toast.makeText(context, "دستاويز محفوظ ٿي ويو (Document Saved)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Two primary tabs: "Enhanced Image" and "Extracted Sindhi Text"
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("doc_processing_tabs")
            ) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = {
                        Text(
                            text = "Enhanced Image (تصوير)",
                            fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("tab_enhanced_image")
                )
                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = {
                        Text(
                            text = "Extracted Sindhi Text (سنڌي لکت)",
                            fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.testTag("tab_extracted_text")
                )
            }

            // Tab Content
            when (uiState.selectedTabIndex) {
                0 -> EnhancedImageTabContent(
                    uiState = uiState,
                    onFilterSelected = { viewModel.applyFilter(it) },
                    onRotate = { viewModel.rotateImage() },
                    onCropClick = { viewModel.toggleCropDialog(true) },
                    onPdfPageChange = { viewModel.goToPdfPage(it) },
                    onRunOcr = { viewModel.runOcr() }
                )
                1 -> ExtractedTextTabContent(
                    uiState = uiState,
                    fontSize = sindhiFontSize,
                    onFontSizeChange = { sindhiFontSize = it },
                    onTextChange = { viewModel.updateExtractedText(it) },
                    onCharInserted = { viewModel.insertSindhiCharacter(it) },
                    onRetryOcr = { viewModel.runOcr() },
                    onOpenApiKeyDialog = { viewModel.toggleApiKeyDialog(true) },
                    onSaveDoc = {
                        viewModel.saveDocument { success ->
                            if (success) {
                                Toast.makeText(context, "دستاويز محفوظ ٿي ويو (Document Saved)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Interactive Crop Dialog
    if (uiState.showCropDialog && uiState.originalBitmap != null) {
        DocumentCropDialog(
            bitmap = uiState.originalBitmap,
            onApplyCrop = { left, top, right, bottom ->
                viewModel.applyCrop(left, top, right, bottom)
            },
            onDismiss = { viewModel.toggleCropDialog(false) }
        )
    }

    if (uiState.showApiKeyDialog) {
        ApiKeySettingsDialog(
            currentApiKey = uiState.customApiKey,
            onSaveKey = { key ->
                viewModel.saveCustomApiKey(key)
            },
            onDismiss = { viewModel.toggleApiKeyDialog(false) }
        )
    }

    if (showTitleDialog) {
        var newTitle by remember { mutableStateOf(uiState.currentDocTitle) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title (عنوان)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDocumentTitle(newTitle)
                        showTitleDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EnhancedImageTabContent(
    uiState: DocScanUiState,
    onFilterSelected: (DocumentImageFilter) -> Unit,
    onRotate: () -> Unit,
    onCropClick: () -> Unit,
    onPdfPageChange: (Int) -> Unit,
    onRunOcr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Multi-page PDF Navigator if current document is a PDF
        if (uiState.isPdf) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("pdf_page_navigator")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onPdfPageChange(uiState.currentPdfPage - 1) },
                        enabled = uiState.currentPdfPage > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pdf_prev_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Previous Page",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev Page", fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Page ${uiState.currentPdfPage + 1} of ${uiState.pdfPageCount}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "پيج تبديل ڪريو",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    FilledTonalButton(
                        onClick = { onPdfPageChange(uiState.currentPdfPage + 1) },
                        enabled = uiState.currentPdfPage < uiState.pdfPageCount - 1,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pdf_next_page_button")
                    ) {
                        Text("Next Page", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Next Page",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Document Image Display Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .testTag("document_preview_container")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val displayBitmap = uiState.filteredBitmap ?: uiState.originalBitmap
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Enhanced Document Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("document_preview_image")
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No image selected",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Filtering loader overlay
                if (uiState.isFiltering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Action overlays: Rotate and Crop buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Crop Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp
                    ) {
                        IconButton(
                            onClick = onCropClick,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("crop_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Rotate Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 3.dp
                    ) {
                        IconButton(
                            onClick = onRotate,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("rotate_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Options Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دستاويز صاف ڪرڻ جا فلٽر (Image Filters)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.activeFilter.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Filter Selection Bar
        FilterSelectorBar(
            selectedFilter = uiState.activeFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Run OCR Primary Action Button (Optimized for Fast Streaming)
        ElevatedButton(
            onClick = onRunOcr,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("run_ocr_button")
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFFFD54F)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚡ Instant Fast OCR (تيز رفتار سنڌي او سي آر)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Streaming extraction • Zero wait time",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun ExtractedTextTabContent(
    uiState: DocScanUiState,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onTextChange: (String) -> Unit,
    onCharInserted: (String) -> Unit,
    onRetryOcr: () -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onSaveDoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats = remember(uiState.extractedSindhiText) {
        TextExportHelper.getStats(uiState.extractedSindhiText)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Real-time Streaming Pulse Indicator
        if (uiState.isStreaming) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "⚡ Ultra-Fast Streaming Active (تيز رفتار سنڌي لکت جاري...)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // OCR In-Progress State (Initial Loading before first chunk arrives)
        if (uiState.isOcrLoading && uiState.extractedSindhiText.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("ocr_loading_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "سنڌي عبارت حاصل ڪئي پئي وڃي...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.ocrProgressStatus.ifBlank { "Gemini 3.5 Flash streaming OCR..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // OCR Error State
        if (uiState.ocrErrorMessage != null && !uiState.isOcrLoading) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("ocr_error_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OCR Processing Notice",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.ocrErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isApiKeyMissing) {
                            Button(
                                onClick = onOpenApiKeyDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.testTag("error_configure_key_button")
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Configure Key")
                            }
                        }

                        OutlinedButton(
                            onClick = onRetryOcr,
                            modifier = Modifier.testTag("retry_ocr_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Fast OCR")
                        }
                    }
                }
            }
        }

        // Header with Font Size Adjuster & Word/Character Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${stats.wordCount} Words",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${stats.charCount} Chars",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${stats.lineCount} Lines",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Font size controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (fontSize > 14f) onFontSizeChange(fontSize - 2f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = "${fontSize.toInt()}sp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                IconButton(
                    onClick = { if (fontSize < 36f) onFontSizeChange(fontSize + 2f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sindhi Text Editor in Right-to-Left (RTL) Layout Direction
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            OutlinedTextField(
                value = uiState.extractedSindhiText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .testTag("sindhi_text_editor"),
                placeholder = {
                    Text(
                        text = "حاصل ڪيل سنڌي عبارت هتي ظاهر ٿيندي ۽ اوهان ان کي تبديل ڪري سگهو ٿا...\n(Extracted Sindhi text will appear here. You can freely edit, format, or type.)",
                        textAlign = TextAlign.Right,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                textStyle = TextStyle(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.55f).sp,
                    fontFamily = FontFamily.SansSerif,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sindhi Special Letters and Diacritics Bar
        SindhiDiacriticsBar(
            onCharSelected = onCharInserted,
            modifier = Modifier.testTag("sindhi_diacritics_bar")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row: Copy Text, Share via WhatsApp, Save as TXT
        Text(
            text = "ايڪشن ۽ ايڪسپورٽ (Export Actions)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Action 1: Copy Text
            Button(
                onClick = {
                    TextExportHelper.copyToClipboard(context, uiState.extractedSindhiText)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("copy_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Text",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Text", fontSize = 13.sp)
            }

            // Action 2: Share via WhatsApp
            Button(
                onClick = {
                    TextExportHelper.shareViaWhatsApp(context, uiState.extractedSindhiText)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("share_whatsapp_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share via WhatsApp",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("WhatsApp", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Action 3: Save as TXT
            OutlinedButton(
                onClick = {
                    TextExportHelper.shareOrSaveTxtFile(
                        context = context,
                        text = uiState.extractedSindhiText,
                        baseTitle = uiState.currentDocTitle
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_txt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Save as TXT",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save as TXT", fontSize = 13.sp)
            }

            // Action 4: Save to Database / History
            FilledTonalButton(
                onClick = onSaveDoc,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Save to History",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Scan", fontSize = 13.sp)
            }
        }
    }
}
