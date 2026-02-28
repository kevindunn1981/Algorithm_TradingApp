package com.algotrader.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.algotrader.app.ui.theme.LossRed
import com.algotrader.app.ui.theme.ProfitGreen

@Composable
fun SparklineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    strokeWidth: Float = 2f
) {
    if (data.size < 2) return

    val color = lineColor ?: if (data.last() >= data.first()) ProfitGreen else LossRed

    Canvas(modifier = modifier.fillMaxWidth().height(40.dp)) {
        val maxVal = data.max()
        val minVal = data.min()
        val range = if (maxVal != minVal) maxVal - minVal else 1.0
        val stepX = size.width / (data.size - 1)
        val padding = 4f

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = padding + (1 - (value - minVal) / range).toFloat() * (size.height - 2 * padding)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color, style = Stroke(width = strokeWidth))
    }
}

@Composable
fun EquityCurveChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillAlpha: Float = 0.1f
) {
    if (data.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val maxVal = data.max()
        val minVal = data.min()
        val range = if (maxVal != minVal) maxVal - minVal else 1.0
        val stepX = size.width / (data.size - 1)
        val padding = 8f

        val linePath = Path()
        val fillPath = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = padding + (1 - (value - minVal) / range).toFloat() * (size.height - 2 * padding)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(fillPath, lineColor.copy(alpha = fillAlpha))
        drawPath(linePath, lineColor, style = Stroke(width = 2.5f))
    }
}

@Composable
fun CandlestickChart(
    opens: List<Double>,
    highs: List<Double>,
    lows: List<Double>,
    closes: List<Double>,
    modifier: Modifier = Modifier
) {
    if (opens.isEmpty()) return

    Canvas(modifier = modifier.fillMaxWidth().height(250.dp)) {
        val allValues = highs + lows
        val maxVal = allValues.max()
        val minVal = allValues.min()
        val range = if (maxVal != minVal) maxVal - minVal else 1.0
        val candleWidth = (size.width / opens.size) * 0.7f
        val padding = 8f

        fun priceToY(price: Double): Float =
            padding + (1 - (price - minVal) / range).toFloat() * (size.height - 2 * padding)

        for (i in opens.indices) {
            val x = (i + 0.5f) * (size.width / opens.size)
            val isUp = closes[i] >= opens[i]
            val color = if (isUp) ProfitGreen else LossRed

            drawLine(
                color = color,
                start = Offset(x, priceToY(highs[i])),
                end = Offset(x, priceToY(lows[i])),
                strokeWidth = 1.5f
            )

            val bodyTop = priceToY(if (isUp) closes[i] else opens[i])
            val bodyBottom = priceToY(if (isUp) opens[i] else closes[i])
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1f)

            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth / 2, bodyTop),
                size = androidx.compose.ui.geometry.Size(candleWidth, bodyHeight)
            )
        }
    }
}
