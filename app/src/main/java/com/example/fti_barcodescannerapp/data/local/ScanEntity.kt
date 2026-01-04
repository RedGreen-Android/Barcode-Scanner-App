package com.example.fti_barcodescannerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawValue: String,
    val format: Int,
    val timestampMs: Long
)