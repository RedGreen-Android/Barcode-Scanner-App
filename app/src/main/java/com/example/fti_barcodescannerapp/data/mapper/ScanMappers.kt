package com.example.fti_barcodescannerapp.data.mapper

import com.example.fti_barcodescannerapp.data.local.ScanEntity
import com.example.fti_barcodescannerapp.domain.model.Scan

fun ScanEntity.toDomain() = Scan(
    id = id,
    rawValue = rawValue,
    format = format,
    timestampMs = timestampMs
)

fun Scan.toEntity() = ScanEntity(
    id = id,
    rawValue = rawValue,
    format = format,
    timestampMs = timestampMs
)