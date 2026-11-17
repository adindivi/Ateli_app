package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.data.model.Garment

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF3E4A61)): Color {
    return try {
        val cleaned = hex.trim()
        Color(AndroidColor.parseColor(if (cleaned.startsWith("#")) cleaned else "#$cleaned"))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Renders a tailored, high-fashion vector garment cutout ("누끼 프리뷰") on a transparent or studio checkerboard backdrop.
 * If a camera cutout bitmap exists in memory for this garment, it renders the actual cutout bitmap;
 * otherwise it draws a bespoke vector silhouette in the garment's exact colorHex, pattern, and category.
 */
@Composable
fun GarmentCutoutIllustration(
    garment: Garment,
    cutoutBitmap: Bitmap? = null,
    showCheckerboard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = parseHexColorSafe(garment.colorHex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showCheckerboard) {
                drawTransparentCheckerboard()
            }
            if (cutoutBitmap == null) {
                drawGarmentSilhouette(
                    category = garment.category,
                    subCategory = garment.subCategory,
                    baseColor = primaryColor,
                    pattern = garment.pattern
                )
            }
        }
        if (cutoutBitmap != null) {
            Image(
                bitmap = cutoutBitmap.asImageBitmap(),
                contentDescription = garment.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun DrawScope.drawTransparentCheckerboard() {
    val cell = 12.dp.toPx()
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    val light = Color(0xFFF3EFEA)
    val dark = Color(0xFFE5DFD5)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            drawRect(
                color = if ((r + c) % 2 == 0) light else dark,
                topLeft = Offset(c * cell, r * cell),
                size = Size(cell, cell)
            )
        }
    }
}

fun DrawScope.drawGarmentSilhouette(
    category: String,
    subCategory: String,
    baseColor: Color,
    pattern: String
) {
    val w = size.width
    val h = size.height
    val strokeColor = Color.Black.copy(alpha = 0.28f)
    val highlightColor = Color.White.copy(alpha = 0.22f)

    when (category) {
        "OUTER" -> {
            // Tailored Trench Coat / Blazer Silhouette with Lapels
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.12f)
                lineTo(w * 0.72f, h * 0.12f)
                lineTo(w * 0.90f, h * 0.26f)
                lineTo(w * 0.84f, h * 0.78f)
                lineTo(w * 0.70f, h * 0.76f)
                lineTo(w * 0.72f, h * 0.34f)
                lineTo(w * 0.74f, h * 0.90f)
                lineTo(w * 0.26f, h * 0.90f)
                lineTo(w * 0.28f, h * 0.34f)
                lineTo(w * 0.30f, h * 0.76f)
                lineTo(w * 0.16f, h * 0.78f)
                lineTo(w * 0.10f, h * 0.26f)
                close()
            }
            drawPath(path, color = baseColor)
            drawPath(path, color = strokeColor, style = Stroke(width = 3f))

            // Lapel V-lines & buttons
            val lapelPath = Path().apply {
                moveTo(w * 0.38f, h * 0.12f)
                lineTo(w * 0.50f, h * 0.42f)
                lineTo(w * 0.62f, h * 0.12f)
            }
            drawPath(lapelPath, color = strokeColor, style = Stroke(width = 3f))
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.50f, h * 0.42f),
                end = Offset(w * 0.50f, h * 0.90f),
                strokeWidth = 2.5f
            )
            // Double-breasted buttons
            for (row in 0..2) {
                val by = h * (0.48f + row * 0.12f)
                drawCircle(color = strokeColor, radius = 4f, center = Offset(w * 0.44f, by))
                drawCircle(color = strokeColor, radius = 4f, center = Offset(w * 0.56f, by))
            }
        }

        "TOP" -> {
            // Shirt / Knit Pullover Silhouette
            val path = Path().apply {
                moveTo(w * 0.30f, h * 0.15f)
                lineTo(w * 0.70f, h * 0.15f)
                lineTo(w * 0.88f, h * 0.32f)
                lineTo(w * 0.80f, h * 0.68f)
                lineTo(w * 0.68f, h * 0.66f)
                lineTo(w * 0.68f, h * 0.36f)
                lineTo(w * 0.68f, h * 0.84f)
                lineTo(w * 0.32f, h * 0.84f)
                lineTo(w * 0.32f, h * 0.36f)
                lineTo(w * 0.32f, h * 0.66f)
                lineTo(w * 0.20f, h * 0.68f)
                lineTo(w * 0.12f, h * 0.32f)
                close()
            }
            drawPath(path, color = baseColor)

            if (pattern.contains("스트라이프") || subCategory.contains("스트라이프")) {
                for (i in 3..7) {
                    val x = w * (i * 0.09f)
                    drawLine(
                        color = highlightColor,
                        start = Offset(x, h * 0.18f),
                        end = Offset(x, h * 0.82f),
                        strokeWidth = 3f
                    )
                }
            } else if (pattern.contains("케이블") || subCategory.contains("니트")) {
                for (i in 4..6) {
                    val x = w * (i * 0.10f)
                    drawLine(
                        color = strokeColor.copy(alpha = 0.18f),
                        start = Offset(x, h * 0.20f),
                        end = Offset(x, h * 0.82f),
                        strokeWidth = 4f
                    )
                }
            }

            drawPath(path, color = strokeColor, style = Stroke(width = 3f))
            // Collar
            drawArc(
                color = strokeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.40f, h * 0.10f),
                size = Size(w * 0.20f, h * 0.10f),
                style = Stroke(width = 3f)
            )
        }

        "BOTTOM" -> {
            // Tailored Wide Slacks / Denim Silhouette
            val path = Path().apply {
                moveTo(w * 0.30f, h * 0.10f)
                lineTo(w * 0.70f, h * 0.10f)
                lineTo(w * 0.76f, h * 0.90f)
                lineTo(w * 0.53f, h * 0.90f)
                lineTo(w * 0.50f, h * 0.36f)
                lineTo(w * 0.47f, h * 0.90f)
                lineTo(w * 0.24f, h * 0.90f)
                close()
            }
            drawPath(path, color = baseColor)
            drawPath(path, color = strokeColor, style = Stroke(width = 3f))
            // Waistband & Pleat lines
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.30f, h * 0.17f),
                end = Offset(w * 0.70f, h * 0.17f),
                strokeWidth = 2.5f
            )
            drawLine(
                color = highlightColor,
                start = Offset(w * 0.38f, h * 0.20f),
                end = Offset(w * 0.36f, h * 0.86f),
                strokeWidth = 2f
            )
            drawLine(
                color = highlightColor,
                start = Offset(w * 0.62f, h * 0.20f),
                end = Offset(w * 0.64f, h * 0.86f),
                strokeWidth = 2f
            )
        }

        "SHOES" -> {
            // Pair of Stylized Loafers / Sneakers
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(w * 0.14f, h * 0.42f),
                size = Size(w * 0.32f, h * 0.28f),
                cornerRadius = CornerRadius(22f, 22f)
            )
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(w * 0.54f, h * 0.42f),
                size = Size(w * 0.32f, h * 0.28f),
                cornerRadius = CornerRadius(22f, 22f)
            )
            // Outsole
            drawRoundRect(
                color = Color(0xFFEAE6DF),
                topLeft = Offset(w * 0.12f, h * 0.66f),
                size = Size(w * 0.36f, h * 0.08f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0xFFEAE6DF),
                topLeft = Offset(w * 0.52f, h * 0.66f),
                size = Size(w * 0.36f, h * 0.08f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        else -> {
            // Minimalist Leather Bag
            drawArc(
                color = strokeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.35f, h * 0.16f),
                size = Size(w * 0.30f, h * 0.28f),
                style = Stroke(width = 5f)
            )
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(w * 0.24f, h * 0.32f),
                size = Size(w * 0.52f, h * 0.48f),
                cornerRadius = CornerRadius(24f, 24f)
            )
        }
    }
}
