package com.labflow.companion

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class ScannerOverlayView(
    context: Context,
    private val colors: CompanionColors
) : View(context) {

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(158, 7, 10, 15)
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.primary
        style = Paint.Style.STROKE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(dp(10f))
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mix(colors.primary, Color.WHITE, 0.18f)
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255)
        textSize = dp(14f)
        textAlign = Paint.Align.CENTER
    }

    private var scanProgress = 0f
    private var pulse = 0f
    private var flashStrength = 0f
    private var detected = false

    private val scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1850L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            scanProgress = it.animatedValue as Float
            invalidate()
        }
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0.72f, 1f).apply {
        duration = 1300L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }

    private val flashAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
        duration = 220L
        interpolator = LinearInterpolator()
        addUpdateListener {
            flashStrength = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!scanAnimator.isStarted) scanAnimator.start()
        if (!pulseAnimator.isStarted) pulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        scanAnimator.cancel()
        pulseAnimator.cancel()
        flashAnimator.cancel()
        super.onDetachedFromWindow()
    }

    fun flashSuccess() {
        detected = true
        cornerPaint.color = colors.success
        linePaint.color = mix(colors.success, Color.WHITE, 0.15f)
        flashAnimator.cancel()
        flashAnimator.start()
        postDelayed({
            detected = false
            cornerPaint.color = colors.primary
            linePaint.color = mix(colors.primary, Color.WHITE, 0.18f)
            invalidate()
        }, 360L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scanRect = scanRect()

        val overlayPath = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRoundRect(scanRect, dp(26f), dp(26f), Path.Direction.CW)
        }
        canvas.drawPath(overlayPath, overlayPaint)

        if (flashStrength > 0f) {
            canvas.drawColor(Color.argb((flashStrength * 110).toInt(), 50, 209, 88))
        }

        drawCorners(canvas, scanRect)
        drawScanLine(canvas, scanRect)
        drawGuideText(canvas, scanRect)
    }

    private fun drawCorners(canvas: Canvas, scanRect: RectF) {
        val length = scanRect.width() * 0.18f
        cornerPaint.alpha = (190 + 55 * pulse).toInt().coerceIn(0, 255)

        // top left
        canvas.drawLine(scanRect.left, scanRect.top + length, scanRect.left, scanRect.top, cornerPaint)
        canvas.drawLine(scanRect.left, scanRect.top, scanRect.left + length, scanRect.top, cornerPaint)
        // top right
        canvas.drawLine(scanRect.right - length, scanRect.top, scanRect.right, scanRect.top, cornerPaint)
        canvas.drawLine(scanRect.right, scanRect.top, scanRect.right, scanRect.top + length, cornerPaint)
        // bottom left
        canvas.drawLine(scanRect.left, scanRect.bottom - length, scanRect.left, scanRect.bottom, cornerPaint)
        canvas.drawLine(scanRect.left, scanRect.bottom, scanRect.left + length, scanRect.bottom, cornerPaint)
        // bottom right
        canvas.drawLine(scanRect.right - length, scanRect.bottom, scanRect.right, scanRect.bottom, cornerPaint)
        canvas.drawLine(scanRect.right, scanRect.bottom - length, scanRect.right, scanRect.bottom, cornerPaint)
    }

    private fun drawScanLine(canvas: Canvas, scanRect: RectF) {
        val y = scanRect.top + scanRect.height() * (0.08f + 0.84f * scanProgress)
        linePaint.alpha = if (detected) 255 else 220
        canvas.drawLine(scanRect.left + dp(16f), y, scanRect.right - dp(16f), y, linePaint)
    }

    private fun drawGuideText(canvas: Canvas, scanRect: RectF) {
        canvas.drawText(
            "Align QR code within frame",
            width / 2f,
            scanRect.bottom + dp(34f),
            textPaint
        )
    }

    private fun scanRect(): RectF {
        val size = min(width * 0.74f, height * 0.42f).coerceAtLeast(dp(220f))
        val left = (width - size) / 2f
        val top = (height - size) / 2f - dp(26f)
        return RectF(left, top, left + size, top + size)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun mix(start: Int, end: Int, ratio: Float): Int {
        val safe = ratio.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(start) + ((Color.red(end) - Color.red(start)) * safe)).toInt(),
            (Color.green(start) + ((Color.green(end) - Color.green(start)) * safe)).toInt(),
            (Color.blue(start) + ((Color.blue(end) - Color.blue(start)) * safe)).toInt()
        )
    }
}
