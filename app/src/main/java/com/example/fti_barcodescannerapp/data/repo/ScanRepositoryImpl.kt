package com.example.fti_barcodescannerapp.data.repo

import com.example.fti_barcodescannerapp.data.local.ScanDao
import com.example.fti_barcodescannerapp.data.mapper.toDomain
import com.example.fti_barcodescannerapp.data.mapper.toEntity
import com.example.fti_barcodescannerapp.domain.model.Scan
import com.example.fti_barcodescannerapp.domain.repo.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val dao: ScanDao
) : ScanRepository {

    override suspend fun saveScan(scan: Scan) {
        dao.insert(scan.toEntity())
    }

    override fun observeRecentScans(limit: Int): Flow<List<Scan>> {
        return dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
    }
}