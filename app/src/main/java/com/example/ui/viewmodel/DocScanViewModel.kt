package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ScannedDocument
import com.example.data.network.GeminiOcrService
import com.example.data.network.OcrResult
import com.example.util.DocumentImageFilter
import com.example.util.DocumentImageProcessor
import com.example.util.PdfDocumentHelper
import com.example.util.SindhiScriptHelper
import com.example.util.TextExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    PROCESS_DOC
}

data class DocScanUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedImageUri: Uri? = null,
    val isPdf: Boolean = false,
    val pdfUri: Uri? = null,
    val pdfPageCount: Int = 1,
    val currentPdfPage: Int = 0,
    val originalBitmap: Bitmap? = null,
    val filteredBitmap: Bitmap? = null,
    val activeFilter: DocumentImageFilter = DocumentImageFilter.ORIGINAL,
    val rotationAngle: Float = 0f,
    val selectedTabIndex: Int = 0, // 0: Enhanced Image, 1: Extracted Sindhi Text
    val isFiltering: Boolean = false,
    val isOcrLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val ocrProgressStatus: String = "",
    val extractedSindhiText: String = "",
    val ocrErrorMessage: String? = null,
    val isApiKeyMissing: Boolean = false,
    val customApiKey: String = "",
    val showApiKeyDialog: Boolean = false,
    val showCropDialog: Boolean = false,
    val currentDocTitle: String = "سنڌي دستاويز",
    val currentDocId: Long? = null,
    val isSavedSuccess: Boolean = false
)

class DocScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val docDao = db.documentDao()
    private val prefs = application.getSharedPreferences("sindhi_docscan_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        DocScanUiState(
            customApiKey = prefs.getString("custom_api_key", "").orEmpty()
        )
    )
    val uiState: StateFlow<DocScanUiState> = _uiState.asStateFlow()

    val savedDocuments: StateFlow<List<ScannedDocument>> = docDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val geminiService = GeminiOcrService {
        _uiState.value.customApiKey
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("custom_api_key", trimmed).apply()
        _uiState.value = _uiState.value.copy(
            customApiKey = trimmed,
            showApiKeyDialog = false,
            isApiKeyMissing = false,
            ocrErrorMessage = null
        )
    }

    fun toggleApiKeyDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = show)
    }

    fun toggleCropDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCropDialog = show)
    }

    fun navigateToHome() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.HOME)
    }

    fun setSelectedTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFiltering = true,
                ocrErrorMessage = null,
                selectedTabIndex = 0,
                currentDocId = null,
                isPdf = false,
                pdfUri = null,
                extractedSindhiText = "",
                rotationAngle = 0f,
                activeFilter = DocumentImageFilter.MAGIC_BW
            )

            val bitmap = DocumentImageProcessor.loadBitmapFromUri(getApplication(), uri)
            if (bitmap != null) {
                val filtered = DocumentImageProcessor.applyFilter(bitmap, DocumentImageFilter.MAGIC_BW)
                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.PROCESS_DOC,
                    selectedImageUri = uri,
                    originalBitmap = bitmap,
                    filteredBitmap = filtered,
                    isFiltering = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFiltering = false,
                    ocrErrorMessage = "Failed to load image from source."
                )
            }
        }
    }

    fun onPdfSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFiltering = true,
                ocrErrorMessage = null,
                selectedTabIndex = 0,
                currentDocId = null,
                isPdf = true,
                pdfUri = uri,
                currentPdfPage = 0,
                extractedSindhiText = "",
                rotationAngle = 0f,
                activeFilter = DocumentImageFilter.MAGIC_BW
            )

            val meta = PdfDocumentHelper.getPdfMeta(getApplication(), uri)
            val pageCount = meta?.pageCount ?: 1
            val title = meta?.fileName ?: "Sindhi_Document.pdf"

            val pageBitmap = PdfDocumentHelper.renderPdfPage(getApplication(), uri, 0)
            if (pageBitmap != null) {
                val filtered = DocumentImageProcessor.applyFilter(pageBitmap, DocumentImageFilter.MAGIC_BW)
                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.PROCESS_DOC,
                    currentDocTitle = title,
                    pdfPageCount = pageCount,
                    originalBitmap = pageBitmap,
                    filteredBitmap = filtered,
                    isFiltering = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFiltering = false,
                    ocrErrorMessage = "Could not render PDF document. Please verify the file is not password protected."
                )
            }
        }
    }

    fun goToPdfPage(pageIndex: Int) {
        val uri = _uiState.value.pdfUri ?: return
        if (pageIndex < 0 || pageIndex >= _uiState.value.pdfPageCount) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFiltering = true,
                currentPdfPage = pageIndex
            )

            val pageBitmap = PdfDocumentHelper.renderPdfPage(getApplication(), uri, pageIndex)
            if (pageBitmap != null) {
                val filtered = DocumentImageProcessor.applyFilter(pageBitmap, _uiState.value.activeFilter)
                _uiState.value = _uiState.value.copy(
                    originalBitmap = pageBitmap,
                    filteredBitmap = filtered,
                    isFiltering = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFiltering = false,
                    ocrErrorMessage = "Failed to render PDF page ${pageIndex + 1}"
                )
            }
        }
    }

    fun applyCrop(leftFrac: Float, topFrac: Float, rightFrac: Float, bottomFrac: Float) {
        val base = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFiltering = true, showCropDialog = false)
            val cropped = DocumentImageProcessor.cropBitmap(base, leftFrac, topFrac, rightFrac, bottomFrac)
            val filtered = DocumentImageProcessor.applyFilter(cropped, _uiState.value.activeFilter)
            _uiState.value = _uiState.value.copy(
                originalBitmap = cropped,
                filteredBitmap = filtered,
                isFiltering = false
            )
        }
    }

    fun loadSampleDocument(drawableResId: Int, sample: SindhiScriptHelper.SampleDocument) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFiltering = true,
                ocrErrorMessage = null,
                selectedTabIndex = 0,
                currentDocId = null,
                isPdf = false,
                pdfUri = null,
                currentDocTitle = sample.title,
                extractedSindhiText = "",
                rotationAngle = 0f,
                activeFilter = DocumentImageFilter.MAGIC_BW
            )

            val bitmap = try {
                BitmapFactory.decodeResource(getApplication<Application>().resources, drawableResId)
            } catch (e: Exception) {
                null
            }

            if (bitmap != null) {
                val filtered = DocumentImageProcessor.applyFilter(bitmap, DocumentImageFilter.MAGIC_BW)
                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.PROCESS_DOC,
                    selectedImageUri = null,
                    originalBitmap = bitmap,
                    filteredBitmap = filtered,
                    isFiltering = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFiltering = false,
                    ocrErrorMessage = "Failed to load sample document."
                )
            }
        }
    }

    fun applyFilter(filter: DocumentImageFilter) {
        val baseBitmap = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFiltering = true,
                activeFilter = filter
            )
            val rotated = if (_uiState.value.rotationAngle != 0f) {
                DocumentImageProcessor.rotateBitmap(baseBitmap, _uiState.value.rotationAngle)
            } else {
                baseBitmap
            }
            val filtered = DocumentImageProcessor.applyFilter(rotated, filter)
            _uiState.value = _uiState.value.copy(
                filteredBitmap = filtered,
                isFiltering = false
            )
        }
    }

    fun rotateImage() {
        val baseBitmap = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFiltering = true)
            val newAngle = (_uiState.value.rotationAngle + 90f) % 360f
            val rotated = DocumentImageProcessor.rotateBitmap(baseBitmap, newAngle)
            val filtered = DocumentImageProcessor.applyFilter(rotated, _uiState.value.activeFilter)
            _uiState.value = _uiState.value.copy(
                rotationAngle = newAngle,
                filteredBitmap = filtered,
                isFiltering = false
            )
        }
    }

    fun runOcr() {
        val bitmapToScan = _uiState.value.filteredBitmap ?: _uiState.value.originalBitmap
        if (bitmapToScan == null) {
            _uiState.value = _uiState.value.copy(
                ocrErrorMessage = "No document image available to process."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedTabIndex = 1, // Switch to Extracted Sindhi Text tab
                isOcrLoading = true,
                isStreaming = true,
                ocrErrorMessage = null,
                extractedSindhiText = "",
                ocrProgressStatus = "⚡ Fast OCR with Gemini 3.5 Streaming..."
            )

            val result = geminiService.streamExtractSindhiText(bitmapToScan) { chunk ->
                // Real-time chunk append: user sees words appearing immediately!
                _uiState.value = _uiState.value.copy(
                    extractedSindhiText = _uiState.value.extractedSindhiText + chunk,
                    isOcrLoading = false // Stop blocking loader as soon as first chunk arrives
                )
            }

            when (result) {
                is OcrResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOcrLoading = false,
                        isStreaming = false,
                        extractedSindhiText = if (_uiState.value.extractedSindhiText.isBlank()) result.text else _uiState.value.extractedSindhiText,
                        ocrErrorMessage = null
                    )
                }
                is OcrResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOcrLoading = false,
                        isStreaming = false,
                        ocrErrorMessage = result.message,
                        isApiKeyMissing = result.isApiKeyMissing
                    )
                }
            }
        }
    }

    fun updateExtractedText(newText: String) {
        _uiState.value = _uiState.value.copy(extractedSindhiText = newText)
    }

    fun updateDocumentTitle(title: String) {
        _uiState.value = _uiState.value.copy(currentDocTitle = title)
    }

    fun insertSindhiCharacter(char: String) {
        val currentText = _uiState.value.extractedSindhiText
        _uiState.value = _uiState.value.copy(extractedSindhiText = currentText + char)
    }

    fun saveDocument(onComplete: (Boolean) -> Unit = {}) {
        val state = _uiState.value
        val textToSave = state.extractedSindhiText.trim()
        if (textToSave.isEmpty()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            val stats = TextExportHelper.getStats(textToSave)
            var imagePath: String? = null

            state.filteredBitmap?.let { bitmap ->
                val cachedFile = DocumentImageProcessor.saveBitmapToCache(getApplication(), bitmap)
                imagePath = cachedFile.absolutePath
            }

            val doc = ScannedDocument(
                id = state.currentDocId ?: 0L,
                title = state.currentDocTitle.ifBlank { "سنڌي دستاويز" },
                extractedText = textToSave,
                filterType = state.activeFilter.id,
                imagePath = imagePath,
                wordCount = stats.wordCount,
                charCount = stats.charCount
            )

            val newId = docDao.insertDocument(doc)
            _uiState.value = _uiState.value.copy(
                currentDocId = if (state.currentDocId == null) newId else state.currentDocId,
                isSavedSuccess = true
            )
            onComplete(true)
        }
    }

    fun openSavedDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            val bitmap = if (!doc.imagePath.isNullOrBlank()) {
                BitmapFactory.decodeFile(doc.imagePath)
            } else null

            val filter = DocumentImageFilter.entries.firstOrNull { it.id == doc.filterType }
                ?: DocumentImageFilter.ORIGINAL

            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.PROCESS_DOC,
                currentDocId = doc.id,
                currentDocTitle = doc.title,
                extractedSindhiText = doc.extractedText,
                selectedImageUri = null,
                isPdf = false,
                pdfUri = null,
                originalBitmap = bitmap,
                filteredBitmap = bitmap,
                activeFilter = filter,
                selectedTabIndex = 1
            )
        }
    }

    fun deleteDocument(doc: ScannedDocument) {
        viewModelScope.launch {
            docDao.deleteDocument(doc)
        }
    }
}
