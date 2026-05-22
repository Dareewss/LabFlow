package com.labflow.companion

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class StarFieldView(context: Context, private val colors: CompanionColors) : View(context) {
    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        val alpha: Float,
        val phase: Float,
        val speed: Float,
        val pulseSpeed: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stars = Random(42).let { random ->
        List(54) {
            Star(
                x = 0.04f + random.nextFloat() * 0.92f,
                y = 0.05f + random.nextFloat() * 0.88f,
                size = 1.0f + random.nextFloat() * 3.2f,
                alpha = 0.12f + random.nextFloat() * 0.22f,
                phase = random.nextFloat() * Math.PI.toFloat() * 2f,
                speed = 0.004f + random.nextFloat() * 0.014f,
                pulseSpeed = 0.48f + random.nextFloat() * 1.8f
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) {
            return
        }

        val time = System.nanoTime() / 1_000_000_000.0f
        canvas.drawColor(colors.background)
        drawGlow(canvas, width * 0.50f, height * 0.22f, width * 0.86f, colors.starGlowA)
        drawGlow(canvas, width * 0.17f, height * 0.72f, width * 0.60f, colors.starGlowB)
        drawGlow(canvas, width * 0.86f, height * 0.66f, width * 0.54f, colors.starGlowC)

        stars.forEach { star ->
            val pulse = max(0f, sin(time * star.pulseSpeed + star.phase)).toDouble().pow(5.0).toFloat()
            val alpha = min(1f, star.alpha + pulse * 0.30f)
            val x = star.x * width
            val y = (((star.y + time * star.speed) % 1.08f) - 0.04f) * height
            val size = star.size * (1f + pulse * 0.18f)
            paint.shader = null
            paint.color = Color.argb((alpha * 255).toInt(), Color.red(colors.starColor), Color.green(colors.starColor), Color.blue(colors.starColor))
            canvas.drawCircle(x, y, size, paint)
            paint.color = Color.argb((alpha * 56).toInt(), Color.red(colors.starColor), Color.green(colors.starColor), Color.blue(colors.starColor))
            canvas.drawCircle(x, y, size * 3.4f, paint)
        }
        postInvalidateOnAnimation()
    }

    private fun drawGlow(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.shader = RadialGradient(
            x,
            y,
            radius,
            color,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }
}
