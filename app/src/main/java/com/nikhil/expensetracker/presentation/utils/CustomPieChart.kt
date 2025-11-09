package com.nikhil.expensetracker.presentation.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class CustomPieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var chartData: List<PieSlice> = emptyList()
    private val rectF = RectF()

    private val colors = listOf(
        Color.parseColor("#FF6B6B"), // Red
        Color.parseColor("#4ECDC4"), // Teal
        Color.parseColor("#45B7D1"), // Blue
        Color.parseColor("#F7DC6F"), // Yellow
        Color.parseColor("#BB8FCE"), // Purple
        Color.parseColor("#85C1E9"), // Light Blue
        Color.parseColor("#F8C471"), // Orange
        Color.parseColor("#82E0AA"), // Green
        Color.parseColor("#F1948A"), // Pink
        Color.parseColor("#D7DBDD")  // Gray
    )

    init {
        textPaint.apply {
            color = Color.BLACK
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }

        linePaint.apply {
            color = Color.GRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
    }

    data class PieSlice(
        val label: String,
        val value: Double,
        val percentage: Float,
        val color: Int
    )

    fun setData(categoryBreakdown: Map<String, Double>) {
        val total = categoryBreakdown.values.sum()
        if (total <= 0) {
            chartData = emptyList()
            invalidate()
            return
        }

        chartData = categoryBreakdown.entries.mapIndexed { index, (category, amount) ->
            val percentage = (amount / total * 100).toFloat()
            PieSlice(
                label = category,
                value = amount,
                percentage = percentage,
                color = colors[index % colors.size]
            )
        }.filter { it.percentage > 0 } // Only show slices with data

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (chartData.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) * 0.9f

        // Set up the rectangle for the pie chart
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw pie slices
        var currentAngle = -90f // Start from top
        chartData.forEach { slice ->
            val sweepAngle = slice.percentage / 100f * 360f

            paint.color = slice.color
            paint.style = Paint.Style.FILL
            canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint)

            // Draw slice border
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint)

            // Draw labels for larger slices
            if (slice.percentage > 5f) {
                drawLabel(canvas, slice, currentAngle + sweepAngle / 2, centerX, centerY, radius)
            }

            currentAngle += sweepAngle
        }

        // Draw center circle (donut effect)
        val holeRadius = radius * 0.4f
        paint.color =  Color.parseColor("#6c63f2")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, holeRadius, paint)

        // Draw white stroke around it
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f // Adjust the width as needed
        canvas.drawCircle(centerX, centerY, holeRadius, paint)

        // Draw center text - FIX: Reset text alignment to CENTER before drawing
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 28f
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Expenses", centerX, centerY - 10f, textPaint)

        textPaint.textSize = 24f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Breakdown", centerX, centerY + 20f, textPaint)

        // Draw legend
        drawLegend(canvas)
    }

    private fun drawLabel(
        canvas: Canvas,
        slice: PieSlice,
        angle: Float,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {
        val angleRad = Math.toRadians(angle.toDouble())
        val labelRadius = radius * 0.8f

        val labelX = centerX + (labelRadius * cos(angleRad)).toFloat()
        val labelY = centerY + (labelRadius * sin(angleRad)).toFloat()

        // Save current text alignment
        val currentAlignment = textPaint.textAlign

        textPaint.textSize = 20f
        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.CENTER

        val text = "${slice.percentage.toInt()}%"
        canvas.drawText(text, labelX, labelY, textPaint)

        // Restore text alignment
        textPaint.textAlign = currentAlignment
    }

    private fun drawLegend(canvas: Canvas) {
        val legendStartY = height - 150f
        val legendItemHeight = 30f
        val legendSquareSize = 20f
        val legendTextSize = 24f

        // Save current text properties
        val currentAlignment = textPaint.textAlign
        val currentSize = textPaint.textSize
        val currentColor = textPaint.color
        val currentTypeface = textPaint.typeface

        textPaint.textSize = legendTextSize
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textAlign = Paint.Align.LEFT

        chartData.forEachIndexed { index, slice ->
            val y = legendStartY + index * legendItemHeight

            // Draw color square
            paint.color = slice.color
            paint.style = Paint.Style.FILL
            canvas.drawRect(
                20f,
                y - legendSquareSize / 2,
                20f + legendSquareSize,
                y + legendSquareSize / 2,
                paint
            )

            // Draw label
            val legendText = "${slice.label}: ${slice.percentage.toInt()}%"
            canvas.drawText(legendText, 50f, y + 6f, textPaint)
        }

        // Restore text properties
        textPaint.textAlign = currentAlignment
        textPaint.textSize = currentSize
        textPaint.color = currentColor
        textPaint.typeface = currentTypeface
    }

    private fun drawNoDataMessage(canvas: Canvas) {
        // Save current text properties
        val currentAlignment = textPaint.textAlign
        val currentSize = textPaint.textSize
        val currentColor = textPaint.color
        val currentTypeface = textPaint.typeface

        textPaint.textSize = 32f
        textPaint.color = Color.GRAY
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT

        canvas.drawText(
            "No data available",
            width / 2f,
            height / 2f,
            textPaint
        )

        // Restore text properties
        textPaint.textAlign = currentAlignment
        textPaint.textSize = currentSize
        textPaint.color = currentColor
        textPaint.typeface = currentTypeface
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 600
        val desiredHeight = 500
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}