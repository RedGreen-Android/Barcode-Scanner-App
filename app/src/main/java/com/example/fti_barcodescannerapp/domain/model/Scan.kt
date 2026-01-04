package com.example.fti_barcodescannerapp.domain.model

data class Scan(
    val id: Long = 0L,
    val rawValue: String,
    val format: Int,
    val timestampMs: Long
)