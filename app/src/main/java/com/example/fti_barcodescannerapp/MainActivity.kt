package com.example.fti_barcodescannerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.fti_barcodescannerapp.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

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
        WindowCompat.setDecorFitsSystemWindows(window, true)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun allPermissionsGranted(): Boolean {
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this) //init camerax and binds to lifecycle

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
        val txtResult: TextView = binding.txtResult

        val preview = Preview.Builder()  //Displays live camera feed
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val analysis = ImageAnalysis.Builder()   //If ML Kit is slow → drop old frames, always process latest frame (optimize for fast processing)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetResolution(Size(1280, 720)) // sweet spot for speed + accuracy ..smaller frames = fast processing
            .build()
            .also { imageAnalysis ->
                imageAnalysis.setAnalyzer( //camerax says “For every frame, call analyze(imageProxy).”
                    cameraExecutor,
                    BarcodeAnalyzer { barcodes ->
                        // Take the first barcode and display its rawValue, if it takes 30fps take freshest
                        val first = barcodes.firstOrNull()
                        val value = first?.rawValue ?: "Unknown"
                        runOnUiThread {
                            txtResult.text = "Detected: $value"
                        }
                    }
                )
            }

        try {
            cameraProvider.unbindAll()
            //bind everything to lifecycle so camera stops on pause, releases on destory, no memory leak
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                analysis
            )

            val factory = SurfaceOrientedMeteringPointFactory(
                previewView.width.toFloat(),
                previewView.height.toFloat()
            )

            val point = factory.createPoint(0.5f, 0.5f)
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

//            camera.cameraControl.setFocusMeteringAction(
//                FocusMeteringAction.Builder(
//                    SurfaceOrientedMeteringPointFactory(1f, 1f)
//                        .createPoint(0.5f, 0.5f),
//                    FocusMeteringAction.FLAG_AF
//                ).disableAutoCancel()
//                    .build()
//            )
        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to bind camera use cases", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() //stop background thread and prevent resource leaks
    }
}