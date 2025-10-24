package com.example.evchargingstationapp.utils

import android.content.Context
import android.graphics.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun createMarkerWithLabel(context: Context, text: String): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.BLACK
    paint.textSize = 40f
    paint.textAlign = Paint.Align.CENTER

    val padding = 16
    val textWidth = paint.measureText(text).toInt()
    val width = textWidth + padding * 2
    val height = 60 + padding // height for text box

    val bitmap = Bitmap.createBitmap(width, height + 60, Bitmap.Config.ARGB_8888) // extra 60 for pin
    val canvas = Canvas(bitmap)

    // Draw white box with black border
    val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    rectPaint.color = Color.WHITE
    rectPaint.style = Paint.Style.FILL
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    borderPaint.color = Color.BLACK
    borderPaint.style = Paint.Style.STROKE
    borderPaint.strokeWidth = 4f

    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, 12f, 12f, rectPaint) // white fill
    canvas.drawRoundRect(rect, 12f, 12f, borderPaint) // black border

    // Draw text
    val textY = height / 2f - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(text, width / 2f, textY, paint)

    // Draw marker pin (triangle)
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    pinPaint.color = Color.RED
    val path = Path()
    val centerX = width / 2f
    path.moveTo(centerX, height + 60f) // bottom tip
    path.lineTo(centerX - 20f, height.toFloat()) // left corner
    path.lineTo(centerX + 20f, height.toFloat()) // right corner
    path.close()
    canvas.drawPath(path, pinPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
