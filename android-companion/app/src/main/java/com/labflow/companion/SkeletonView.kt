package com.labflow.companion

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max

class SkeletonView(
    context: Context,
    private val colors: CompanionColors
) : View(context) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.surfaceAlt
    }

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var shimmerOffset = -1f

    private val shimmerAnimator = ValueAnimator.ofFloat(-1f, 1.4f).apply {
        duration = 1200L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            shimmerOffset = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        alpha = 0.92f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!shimmerAnimator.isStarted) {
            shimmerAnimator.start()
        }
    }

    override fun onDetachedFromWindow() {
        shimmerAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = dp(16).toFloat()
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, basePaint)

        if (width <= 0 || height <= 0) {
            return
        }

        val shimmerWidth = max(width * 0.42f, dp(72).toFloat())
        val left = shimmerOffset * width
        val gradient = LinearGradient(
            left,
            0f,
            left + shimmerWidth,
            height.toFloat(),
            intArrayOf(
                withAlpha(colors.surface, 0.08f),
                withAlpha(colors.cardOverlay, 0.55f),
                withAlpha(colors.surface, 0.08f)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        shimmerPaint.shader = gradient
        canvas.drawRoundRect(rect, radius, radius, shimmerPaint)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun withAlpha(color: Int, alphaFraction: Float): Int {
        val alpha = (255 * alphaFraction).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }
}
