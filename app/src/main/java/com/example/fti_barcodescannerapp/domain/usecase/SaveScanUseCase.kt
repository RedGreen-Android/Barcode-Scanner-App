package com.example.fti_barcodescannerapp.domain.usecase

import com.example.fti_barcodescannerapp.domain.model.Scan
import com.example.fti_barcodescannerapp.domain.repo.ScanRepository
import javax.inject.Inject

class SaveScanUseCase @Inject constructor(
    private val repo: ScanRepository
) {
    suspend operator fun invoke(scan: Scan) = repo.saveScan(scan)
}