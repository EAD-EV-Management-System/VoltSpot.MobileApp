package com.example.evchargingstationapp.ui.operator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.OperatorRepository
import com.google.zxing.ResultPoint
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private lateinit var btnClose: ImageButton
    private lateinit var btnPickImage: Button
    private var isScanning = true

    private val IMAGE_PICK_CODE = 101
    private lateinit var operatorRepository: OperatorRepository

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeScanner = findViewById(R.id.barcodeScanner)
        btnClose = findViewById(R.id.btnClose)
        btnPickImage = findViewById(R.id.btnPickImage)

        operatorRepository = OperatorRepository(this)

        btnClose.setOnClickListener { finish() }
        btnPickImage.setOnClickListener { pickImageFromGallery() }

        if (checkCameraPermission()) startScanning() else requestCameraPermission()
    }

    /** Pick image from gallery */
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    /** Handle image result */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                scanQRCodeFromBitmap(bitmap)
            }
        }
    }

    /** Scan QR code from bitmap */
    private fun scanQRCodeFromBitmap(bitmap: Bitmap) {
        try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = com.google.zxing.BinaryBitmap(HybridBinarizer(source))
            val reader = QRCodeReader()
            val result = reader.decode(binaryBitmap)
            val bookingId = result.text
            Toast.makeText(this, "Scanned: $bookingId", Toast.LENGTH_SHORT).show()

            fetchBookingAndOpenDetail(bookingId)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read QR code", Toast.LENGTH_SHORT).show()
        }
    }

    /** Call API to fetch booking and open detail activity */
    private fun fetchBookingAndOpenDetail(bookingId: String) {
        operatorRepository.getBookingById(bookingId) { success, message, booking ->
            runOnUiThread {
                if (success && booking != null) {
                    val intent = Intent(this, OperatorBookingDetailActivity::class.java)
                    intent.putExtra("BOOKING_ID", booking.id)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Booking not found: $message", Toast.LENGTH_LONG).show()
                    isScanning = true // allow scanning again
                }
            }
        }
    }

    /** Camera scanning */
    private fun startScanning() {
        barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null && isScanning) {
                    isScanning = false
                    val bookingId = result.text
                    Toast.makeText(this@QRScannerActivity, "Scanned: $bookingId", Toast.LENGTH_SHORT).show()
                    fetchBookingAndOpenDetail(bookingId)
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

        barcodeScanner.resume()
    }

    /** Permissions */
    private fun checkCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startScanning()
            else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeScanner.pause()
    }
}
