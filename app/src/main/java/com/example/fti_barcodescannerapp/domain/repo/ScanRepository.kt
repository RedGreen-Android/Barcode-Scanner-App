package com.example.fti_barcodescannerapp.domain.repo

import com.example.fti_barcodescannerapp.domain.model.Scan
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    suspend fun saveScan(scan: Scan)
    fun observeRecentScans(limit: Int = 20): Flow<List<Scan>>
}