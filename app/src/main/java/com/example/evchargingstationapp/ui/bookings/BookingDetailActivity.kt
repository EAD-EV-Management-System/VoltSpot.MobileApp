package com.example.evchargingstationapp.ui.bookings

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

class BookingDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        val bookingId = intent.getStringExtra("BOOKING_ID") ?: "Unknown"
        val title = findViewById<TextView>(R.id.bookingDetailTitle)
        val qrImage = findViewById<ImageView>(R.id.qrImage)

        title.text = bookingId
        generateQRCode(bookingId, qrImage)
    }

    private fun generateQRCode(text: String, imageView: ImageView) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        imageView.setImageBitmap(bmp)
    }
}
