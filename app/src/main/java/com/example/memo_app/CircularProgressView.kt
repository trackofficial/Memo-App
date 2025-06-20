package com.example.memo_app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircularProgressView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var progress = 0f  // процент от 0 до 100
    private val strokeWidth = 25f

    // Фон круга (не заполненный прогрессом)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#050C0E13")
        this.strokeWidth = this@CircularProgressView.strokeWidth
    }

    // Кисть для отрисовки прогресса
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#56B530")  // основной цвет прогресса (можно сделать параметром)
        this.strokeWidth = this@CircularProgressView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) - strokeWidth) / 2f

        // Рисуем фон круга
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Массив, описывающий прямоугольник, по которому рисуется дуга
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        // Рисуем прогресс-дугу, начиная с верха (-90°)
        canvas.drawArc(oval, -90f, progress / 100f * 360f, false, progressPaint)
    }
}