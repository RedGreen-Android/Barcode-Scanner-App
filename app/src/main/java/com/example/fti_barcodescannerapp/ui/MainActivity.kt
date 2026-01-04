package com.example.fti_barcodescannerapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.fti_barcodescannerapp.R
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fti_barcodescannerapp.databinding.ActivityMainBinding
import com.example.fti_barcodescannerapp.databinding.DialogScanHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ScanViewModel by viewModels()

    private val cameraExecutor = Executors.newSingleThreadExecutor() //Analyzer runs off the UI thread, single thread = frames processed sequentailly

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
      //  WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.txtResult) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                view.paddingBottom + systemBars.bottom
            )
            insets
        }

        setupToolbar()
        observeViewModel()

        if (hasPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun observeViewModel(){
        viewModel.latestText.observe(this) {
            binding.txtResult.text = it
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_history -> {
                    showScanHistoryDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun hasPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "Camera permission is needed to scan barcodes", Toast.LENGTH_LONG)
                .show()
        }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.Companion.getInstance(this) //init camerax and binds to lifecycle

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val previewView = binding.previewView
//        val txtResult: TextView = binding.txtResult

        val preview = Preview.Builder()  //Displays live camera feed
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

//        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val analysis = ImageAnalysis.Builder()   //If ML Kit is slow → drop old frames, always process latest frame (optimize for fast processing)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetResolution(Size(1280, 720)) // sweet spot for speed + accuracy ..smaller frames = fast processing
            .build() //handles autofocus, exposure, frame rate... initially
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer( //camerax says “For every frame, call analyze(imageProxy).”
                    cameraExecutor,
                    BarcodeAnalyzer { barcodes ->
                        // Take the first barcode and display its rawValue, if it takes 30fps take freshest
//                        val first = barcodes.firstOrNull()
//                        val value = first?.rawValue ?: "Unknown"
//                        runOnUiThread {
//                            txtResult.text = "Detected: $value"
//                        }
                        viewModel.onBarcodesDetected(barcodes)

                    }
                )
            }

        try {
            cameraProvider.unbindAll()
            //bind everything to lifecycle so camera stops on pause, releases on destory, no memory leak
            val camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

//
//            val point = factory.createPoint(0.5f, 0.5f)
//
//            if (camera.cameraInfo.hasFlashUnit()) {
//                camera.cameraControl.enableTorch(true)
//            }

//            camera.cameraControl.setFocusMeteringAction(
//                FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
//                    .disableAutoCancel()
//                    .build()
//            )

            val exposureState = camera.cameraInfo.exposureState
            if (exposureState.isExposureCompensationSupported) {
                camera.cameraControl.setExposureCompensationIndex(
                    (exposureState.exposureCompensationIndex + 1)
                        .coerceAtMost(exposureState.exposureCompensationRange.upper)
                )
            }

            val meteringPointFactory = SurfaceOrientedMeteringPointFactory(
                previewView.width.toFloat(),
                previewView.height.toFloat()
            )

            val centerPoint = meteringPointFactory.createPoint(
                previewView.width / 2f,
                previewView.height / 2f
            )

            val focusAction = FocusMeteringAction.Builder(
                centerPoint,
                FocusMeteringAction.FLAG_AF
            )
                .disableAutoCancel() // keeps focus locked
                .build()

            camera.cameraControl.startFocusAndMetering(focusAction)


//Implemented continuous autofocus using CameraX’s FocusMeteringAction with a center-weighted metering point,
        // ensuring consistent focus during real-time barcode scanning.”

        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to bind camera use cases", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showScanHistoryDialog() {
        val dialogBinding = DialogScanHistoryBinding.inflate(layoutInflater)
        val adapter = ScanHistoryAdapter()

        dialogBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        dialogBinding.recyclerView.adapter = adapter

        viewModel.recentScans.observe(this) { scans ->
            adapter.submitList(scans)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() //stop background thread and prevent resource leaks
    }
}