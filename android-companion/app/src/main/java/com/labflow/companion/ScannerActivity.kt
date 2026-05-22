package com.labflow.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var finishedScan = false
    private lateinit var settings: SettingsStore
    private lateinit var colors: CompanionColors
    private lateinit var overlayView: ScannerOverlayView
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            failAndClose("Camera permission is required to scan QR codes.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsStore(this)
        colors = CompanionTheme.resolve(settings.paletteEnum(), settings.modeEnum())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        try {
            val root = FrameLayout(this).apply {
                setBackgroundColor(colors.background)
            }
            val previewView = PreviewView(this)
            overlayView = ScannerOverlayView(this, colors)

            val label = TextView(this).apply {
                text = "Scan a LabFlow QR code"
                textSize = 17f
                setTextColor(colors.foreground)
                setBackgroundColor(colors.cardOverlay)
                setPadding(dp(28), dp(18), dp(28), dp(18))
            }
            val labelParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dp(28)
            }

            root.addView(previewView)
            root.addView(overlayView)
            root.addView(label, labelParams)
            setContentView(root)

            val providerFuture = ProcessCameraProvider.getInstance(this)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { imageProxy -> analyze(previewView, imageProxy) }
                    provider.unbindAll()
                    provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    failAndClose("Camera could not start.")
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            failAndClose("QR scanner could not start.")
        }
    }

    private fun analyze(previewView: PreviewView, imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage == null || finishedScan) {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    val code = barcodes.firstOrNull()?.rawValue
                    if (!code.isNullOrBlank() && code.startsWith("LABFLOW-EQ-") && !finishedScan) {
                        finishedScan = true
                        runOnUiThread {
                            overlayView.flashSuccess()
                            window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            previewView.postDelayed({
                                setResult(RESULT_OK, Intent().putExtra("qrCode", code))
                                finish()
                            }, 180L)
                        }
                    }
                }
                .addOnFailureListener { }
                .addOnCompleteListener { imageProxy.close() }
        } catch (e: Exception) {
            imageProxy.close()
            failAndClose("QR scanner encountered an error.")
        }
    }

    private fun failAndClose(message: String) {
        if (!finishedScan) {
            finishedScan = true
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
