package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector

enum class DocumentImageFilter(
    val id: String,
    val titleEnglish: String,
    val titleSindhi: String,
    val description: String,
    val icon: ImageVector
) {
    ORIGINAL(
        id = "original",
        titleEnglish = "Original",
        titleSindhi = "اصل تصوير",
        description = "Unfiltered original photo",
        icon = Icons.Default.Image
    ),
    GRAYSCALE(
        id = "grayscale",
        titleEnglish = "Grayscale",
        titleSindhi = "گرين اسڪيل",
        description = "Smooth monochrome tones",
        icon = Icons.Default.FilterBAndW
    ),
    HIGH_CONTRAST(
        id = "high_contrast",
        titleEnglish = "High Contrast",
        titleSindhi = "هاءِ ڪنٽراسٽ",
        description = "Deep dark ink with boosted contrast",
        icon = Icons.Default.Contrast
    ),
    SHADOW_REMOVAL(
        id = "shadow_removal",
        titleEnglish = "Shadow Removal",
        titleSindhi = "پاڇا ۽ داغ هٽايو",
        description = "Cleans paper background and eliminates lighting shadows",
        icon = Icons.Default.AutoAwesome
    ),
    MAGIC_BW(
        id = "magic_bw",
        titleEnglish = "B&W Magic Clean",
        titleSindhi = "جادوئي صاف اسڪين",
        description = "Crisp black document text on clean white sheet",
        icon = Icons.Default.DocumentScanner
    )
}
