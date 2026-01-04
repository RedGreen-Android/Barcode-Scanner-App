package com.example.fti_barcodescannerapp.domain.usecase

import com.example.fti_barcodescannerapp.domain.repo.ScanRepository
import javax.inject.Inject

class ObserveRecentScansUseCase @Inject constructor(
    private val repo: ScanRepository
) {
    operator fun invoke(limit: Int = 20) = repo.observeRecentScans(limit)
}