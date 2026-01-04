package com.example.fti_barcodescannerapp.ui

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer( //CameraX opens the camera, shows preview in PreviewView, every frame to ImageAnalysis.Analyzer
    private val onBarcodeDetected: (List<Barcode>) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS // Tell ML Kit which to detect - QR, Code128, EAN, UPC, etc. (FORMAT_QR_CODE)
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options) //Creates a reusable on-device ML model, ML Kit runs offline, fast, real-time and optimized
    //Create a barcode-scanner engine that lives in (loading barcode detection model into) memory and can process many frames efficiently.

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) { //CameraX calls for every camera frame, Run on every frame and called many times per second
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close() //If null → must close the frame to avoid freezing CameraX. camera hardware always produces frames
            return
        }

        //CameraX delivers frames in YUV format ML kit expects it, skip anything to avoid crash, Only process
        if (imageProxy.format != ImageFormat.YUV_420_888) { // raw camera image format used by Android cameras.
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees) //ML kit handels rotation, mirroring, prespective correction... where is being extracted in overrride
        //bridge between CameraX and ML Kit... knows image bugger, orientation, lens behaviour

        scanner.process(image) //run barcode detection, scans frame (detect edges, correct prespective, decoding, error, structured results)
            .addOnSuccessListener { barcodes -> //List<Barcode> and each barcode contains rawValue, boundingBox, format, valueType
                if (barcodes.isNotEmpty()) {
                    onBarcodeDetected(barcodes)
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeAnalyzer", "Barcode scanning failed", e)
            }
            .addOnCompleteListener { //always close frame else camera freezes, analuzer stops, app crash
                imageProxy.close()
            }
    }
}