package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_documents")
data class ScannedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val extractedText: String,
    val filterType: String,
    val imagePath: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val charCount: Int = 0
)
