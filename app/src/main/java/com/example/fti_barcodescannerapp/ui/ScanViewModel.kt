package com.example.fti_barcodescannerapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.fti_barcodescannerapp.domain.model.Scan
import com.example.fti_barcodescannerapp.R
import com.example.fti_barcodescannerapp.domain.usecase.ObserveRecentScansUseCase
import com.example.fti_barcodescannerapp.domain.usecase.SaveScanUseCase
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    application: Application,
    observeRecentScansUseCase: ObserveRecentScansUseCase,
    private val saveScanUseCase: SaveScanUseCase
) : AndroidViewModel(application) {

    // LiveData for UI
    val recentScans: LiveData<List<Scan>> =
        observeRecentScansUseCase(limit = 20).asLiveData() //re-emits when the DB changes, listens to room..lifecycle aware

    // LiveData for “current detected text”
    private val _latestText = MutableLiveData<String?>()
    val latestText: LiveData<String?> get() = _latestText

    // Simple dedupe to avoid spam
    private var lastValue: String? = null
    private var lastTimeMs: Long = 0L
    private val cooldownMs = 800L

    init {
        _latestText.value = application.getString(R.string.scan_hint)
    }

    fun onBarcodesDetected(barcodes: List<Barcode>) {
        val first = barcodes.firstOrNull() ?: return
        val value = first.rawValue ?: return

        val now = System.currentTimeMillis()
        if (value == lastValue && now - lastTimeMs < cooldownMs) return

        lastValue = value
        lastTimeMs = now

        // _latestText.postValue("Detected: $value")
        _latestText.postValue(
            getApplication<Application>()
                .getString(R.string.scan_detected, value)
        )

        viewModelScope.launch(Dispatchers.IO) {
            saveScanUseCase(
                Scan(
                    rawValue = value,
                    format = first.format,
                    timestampMs = now
                )
            )
        }
    }
}
