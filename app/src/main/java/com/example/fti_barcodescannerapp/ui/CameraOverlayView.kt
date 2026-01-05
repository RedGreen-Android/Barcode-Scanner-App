package com.example.fti_barcodescannerapp.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CameraOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // semi-transparent black
        style = Paint.Style.FILL
    }

    private val framePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var frameRect: RectF? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = frameRect ?: calculateFrameRect().also { frameRect = it }

        // 🔹 Draw dimmed areas AROUND the scan window
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, overlayPaint)               // top
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), overlayPaint) // bottom
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, overlayPaint)           // left
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, overlayPaint) // right

        // 🔹 Draw scan frame
        canvas.drawRect(rect, framePaint)

        // 🔹 Draw corner brackets
        drawCorners(canvas, rect)
    }

    private fun calculateFrameRect(): RectF {
        val size = min(width, height) * 0.65f
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        return RectF(left, top, left + size, top + size)
    }

    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val length = 60f

        // Top-left
        canvas.drawLine(rect.left, rect.top, rect.left + length, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + length, cornerPaint)

        // Top-right
        canvas.drawLine(rect.right, rect.top, rect.right - length, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + length, cornerPaint)

        // Bottom-left
        canvas.drawLine(rect.left, rect.bottom, rect.left + length, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - length, cornerPaint)

        // Bottom-right
        canvas.drawLine(rect.right, rect.bottom, rect.right - length, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - length, cornerPaint)
    }
}
