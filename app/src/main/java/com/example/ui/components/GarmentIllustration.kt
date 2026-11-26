package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CuratedWardrobePresets
import com.example.data.model.Garment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object GarmentBitmapCache {
    private val cache = object : LruCache<String, Bitmap>(32) {}

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF3E4A61)): Color {
    return try {
        val cleaned = hex.trim()
        Color(AndroidColor.parseColor(if (cleaned.startsWith("#")) cleaned else "#$cleaned"))
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Renders a studio-grade real garment cutout photo ("실물 누끼 프리뷰") or user-captured cutout bitmap.
 * Supports:
 * 1. In-memory captured bitmap (`cutoutBitmap`)
 * 2. Bundled transparent cutout assets (`asset://garments/...webp`) & preset auto-resolution
 * 3. Live remote URLs via Coil (`http://` / `https://`)
 * 4. Saved internal storage cutouts (`filesDir/garments/...png`)
 * Falls back cleanly to a tailored vector garment silhouette if no photo asset is available.
 */
@Composable
fun GarmentCutoutIllustration(
    garment: Garment,
    cutoutBitmap: Bitmap? = null,
    showCheckerboard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = parseHexColorSafe(garment.colorHex)
    val resolvedUrl = remember(garment.imageUrl, garment.name, garment.subCategory, garment.category) {
        garment.imageUrl?.takeIf { it.isNotBlank() }
            ?: CuratedWardrobePresets.resolvePresetImageAsset(
                name = garment.name,
                subCategory = garment.subCategory,
                category = garment.category
            )
    }
    val isRemoteUrl = resolvedUrl?.startsWith("http://") == true || resolvedUrl?.startsWith("https://") == true

    val decodedLocalBitmap by produceState<Bitmap?>(
        initialValue = if (cutoutBitmap == null && !isRemoteUrl && resolvedUrl != null) {
            GarmentBitmapCache.get(resolvedUrl)
        } else null,
        key1 = resolvedUrl,
        key2 = cutoutBitmap
    ) {
        if (cutoutBitmap != null || resolvedUrl == null || isRemoteUrl) {
            value = null
            return@produceState
        }
        GarmentBitmapCache.get(resolvedUrl)?.let { cached ->
            value = cached
            return@produceState
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                val bmp = if (resolvedUrl.startsWith("asset://")) {
                    val assetPath = resolvedUrl.removePrefix("asset://")
                    context.assets.open(assetPath).use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val file = java.io.File(resolvedUrl)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                if (bmp != null) {
                    GarmentBitmapCache.put(resolvedUrl, bmp)
                }
                bmp
            } catch (_: Exception) {
                null
            }
        }
        value = loaded
    }

    val effectiveBitmap = cutoutBitmap ?: decodedLocalBitmap
    var remoteLoadFailed by remember(resolvedUrl) { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (showCheckerboard) {
                drawTransparentCheckerboard()
            }
            if (effectiveBitmap == null && (!isRemoteUrl || remoteLoadFailed)) {
                drawGarmentSilhouette(
                    category = garment.category,
                    subCategory = garment.subCategory,
                    baseColor = primaryColor,
                    pattern = garment.pattern
                )
            }
        }
        when {
            effectiveBitmap != null -> {
                Image(
                    bitmap = effectiveBitmap.asImageBitmap(),
                    contentDescription = garment.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            isRemoteUrl && !remoteLoadFailed -> {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = garment.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onError = { remoteLoadFailed = true }
                )
            }
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

    // 1. Apple Studio Ambient Floating Shadow underneath all items
    drawOval(
        color = Color.Black.copy(alpha = 0.06f),
        topLeft = Offset(w * 0.16f, h * 0.88f),
        size = Size(w * 0.68f, h * 0.06f)
    )

    // Tone helpers: no harsh black strokes! Only subtle tone-on-tone or ambient shadows
    val subtleShadow = Color.Black.copy(alpha = 0.10f)
    val subtleHighlight = Color.White.copy(alpha = 0.25f)
    val seamLineColor = Color.Black.copy(alpha = 0.12f)

    when (category) {
        "OUTER" -> {
            // Apple Minimalist Tailored Trench / Blazer Silhouette with smooth Bézier draping
            val outerPath = Path().apply {
                moveTo(w * 0.32f, h * 0.14f)
                // Collar curve
                quadraticTo(w * 0.50f, h * 0.16f, w * 0.68f, h * 0.14f)
                // Right shoulder curve
                cubicTo(w * 0.76f, h * 0.16f, w * 0.84f, h * 0.20f, w * 0.89f, h * 0.26f)
                // Right sleeve outer curve
                quadraticTo(w * 0.86f, h * 0.52f, w * 0.82f, h * 0.74f)
                // Right cuff
                lineTo(w * 0.70f, h * 0.72f)
                // Right underarm inner
                quadraticTo(w * 0.72f, h * 0.50f, w * 0.69f, h * 0.36f)
                // Right body down to hem
                quadraticTo(w * 0.71f, h * 0.62f, w * 0.71f, h * 0.87f)
                // Bottom hem curve
                quadraticTo(w * 0.50f, h * 0.89f, w * 0.29f, h * 0.87f)
                // Left body up to underarm
                quadraticTo(w * 0.29f, h * 0.62f, w * 0.31f, h * 0.36f)
                // Left underarm inner to cuff
                quadraticTo(w * 0.28f, h * 0.50f, w * 0.30f, h * 0.72f)
                // Left cuff
                lineTo(w * 0.18f, h * 0.74f)
                // Left sleeve outer curve
                quadraticTo(w * 0.14f, h * 0.52f, w * 0.11f, h * 0.26f)
                // Left shoulder curve
                cubicTo(w * 0.16f, h * 0.20f, w * 0.24f, h * 0.16f, w * 0.32f, h * 0.14f)
                close()
            }
            drawPath(outerPath, color = baseColor)

            // Inner Collar V-neck cut
            val innerCollar = Path().apply {
                moveTo(w * 0.40f, h * 0.15f)
                lineTo(w * 0.50f, h * 0.40f)
                lineTo(w * 0.60f, h * 0.15f)
                close()
            }
            drawPath(innerCollar, color = Color(0xFFF1ECEA))

            // Lapel fold depth
            val leftLapel = Path().apply {
                moveTo(w * 0.35f, h * 0.14f)
                lineTo(w * 0.50f, h * 0.43f)
                lineTo(w * 0.44f, h * 0.43f)
                lineTo(w * 0.30f, h * 0.26f)
                close()
            }
            drawPath(leftLapel, color = subtleShadow)

            val rightLapel = Path().apply {
                moveTo(w * 0.65f, h * 0.14f)
                lineTo(w * 0.50f, h * 0.43f)
                lineTo(w * 0.56f, h * 0.43f)
                lineTo(w * 0.70f, h * 0.26f)
                close()
            }
            drawPath(rightLapel, color = subtleHighlight)

            // Center front placket line
            drawLine(
                color = seamLineColor,
                start = Offset(w * 0.50f, h * 0.43f),
                end = Offset(w * 0.50f, h * 0.87f),
                strokeWidth = 2f
            )

            // Minimal double buttons
            for (row in 0..1) {
                val by = h * (0.50f + row * 0.14f)
                drawCircle(color = seamLineColor, radius = 3.5f, center = Offset(w * 0.45f, by))
                drawCircle(color = seamLineColor, radius = 3.5f, center = Offset(w * 0.55f, by))
            }
        }

        "TOP" -> {
            // Apple Minimalist Shirt / Knit / Tee Silhouette
            val topPath = Path().apply {
                moveTo(w * 0.35f, h * 0.16f)
                // Soft crew neckline
                quadraticTo(w * 0.50f, h * 0.22f, w * 0.65f, h * 0.16f)
                // Right shoulder
                cubicTo(w * 0.74f, h * 0.18f, w * 0.82f, h * 0.22f, w * 0.86f, h * 0.28f)
                // Right sleeve
                quadraticTo(w * 0.83f, h * 0.48f, w * 0.79f, h * 0.64f)
                // Right cuff
                lineTo(w * 0.68f, h * 0.62f)
                // Underarm
                quadraticTo(w * 0.70f, h * 0.46f, w * 0.67f, h * 0.36f)
                // Body right
                quadraticTo(w * 0.68f, h * 0.58f, w * 0.67f, h * 0.82f)
                // Bottom hem
                quadraticTo(w * 0.50f, h * 0.84f, w * 0.33f, h * 0.82f)
                // Body left
                quadraticTo(w * 0.32f, h * 0.58f, w * 0.33f, h * 0.36f)
                // Left underarm to cuff
                quadraticTo(w * 0.30f, h * 0.46f, w * 0.32f, h * 0.62f)
                // Left cuff
                lineTo(w * 0.21f, h * 0.64f)
                // Left sleeve
                quadraticTo(w * 0.17f, h * 0.48f, w * 0.14f, h * 0.28f)
                // Left shoulder
                cubicTo(w * 0.18f, h * 0.22f, w * 0.26f, h * 0.18f, w * 0.35f, h * 0.16f)
                close()
            }
            drawPath(topPath, color = baseColor)

            // Inner neckline shadow depth
            drawArc(
                color = subtleShadow,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.36f, h * 0.15f),
                size = Size(w * 0.28f, h * 0.10f),
                style = Stroke(width = 3.5f)
            )

            // Texture or subtle pattern
            if (pattern.contains("스트라이프") || subCategory.contains("스트라이프")) {
                for (i in 4..6) {
                    val x = w * (i * 0.10f)
                    drawLine(
                        color = subtleHighlight,
                        start = Offset(x, h * 0.24f),
                        end = Offset(x, h * 0.80f),
                        strokeWidth = 2.5f
                    )
                }
            } else if (pattern.contains("케이블") || subCategory.contains("니트")) {
                for (i in 4..6) {
                    val x = w * (i * 0.10f)
                    drawLine(
                        color = subtleShadow,
                        start = Offset(x, h * 0.26f),
                        end = Offset(x, h * 0.80f),
                        strokeWidth = 3f
                    )
                }
            }
        }

        "BOTTOM" -> {
            // Apple Minimalist Wide Slacks / Denim Silhouette
            val pantsPath = Path().apply {
                moveTo(w * 0.32f, h * 0.12f)
                // Curved waistband
                quadraticTo(w * 0.50f, h * 0.14f, w * 0.68f, h * 0.12f)
                // Right outer leg
                quadraticTo(w * 0.72f, h * 0.48f, w * 0.74f, h * 0.86f)
                // Right cuff
                lineTo(w * 0.54f, h * 0.86f)
                // Inseam crotch
                quadraticTo(w * 0.52f, h * 0.56f, w * 0.50f, h * 0.38f)
                // Left inseam
                quadraticTo(w * 0.48f, h * 0.56f, w * 0.46f, h * 0.86f)
                // Left cuff
                lineTo(w * 0.26f, h * 0.86f)
                // Left outer leg
                quadraticTo(w * 0.28f, h * 0.48f, w * 0.32f, h * 0.12f)
                close()
            }
            drawPath(pantsPath, color = baseColor)

            // Waistband stitch line
            drawLine(
                color = seamLineColor,
                start = Offset(w * 0.32f, h * 0.18f),
                end = Offset(w * 0.68f, h * 0.18f),
                strokeWidth = 2f
            )

            // Center pleats / crease lines
            drawLine(
                color = subtleHighlight,
                start = Offset(w * 0.40f, h * 0.22f),
                end = Offset(w * 0.38f, h * 0.82f),
                strokeWidth = 2f
            )
            drawLine(
                color = subtleHighlight,
                start = Offset(w * 0.60f, h * 0.22f),
                end = Offset(w * 0.62f, h * 0.82f),
                strokeWidth = 2f
            )
        }

        "SHOES" -> {
            // Apple Minimalist Sneaker (Side Profile Sculpture)
            val shoePath = Path().apply {
                moveTo(w * 0.16f, h * 0.66f)
                // Toe box curve
                cubicTo(w * 0.16f, h * 0.56f, w * 0.26f, h * 0.52f, w * 0.38f, h * 0.50f)
                // Instep to ankle
                quadraticTo(w * 0.50f, h * 0.48f, w * 0.58f, h * 0.36f)
                // Collar opening
                quadraticTo(w * 0.68f, h * 0.36f, w * 0.74f, h * 0.40f)
                // Heel curve
                quadraticTo(w * 0.82f, h * 0.46f, w * 0.82f, h * 0.66f)
                // Sole line
                lineTo(w * 0.16f, h * 0.66f)
                close()
            }
            drawPath(shoePath, color = baseColor)

            // Clean modern white outsole
            drawRoundRect(
                color = Color(0xFFF7F7F8),
                topLeft = Offset(w * 0.14f, h * 0.66f),
                size = Size(w * 0.70f, h * 0.12f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Outsole bottom shadow accent
            drawLine(
                color = seamLineColor,
                start = Offset(w * 0.18f, h * 0.78f),
                end = Offset(w * 0.80f, h * 0.78f),
                strokeWidth = 2f
            )

            // Eyelet / Minimal Lace accent
            drawLine(
                color = subtleHighlight,
                start = Offset(w * 0.42f, h * 0.46f),
                end = Offset(w * 0.54f, h * 0.42f),
                strokeWidth = 2.5f
            )
        }

        else -> {
            // Apple Minimalist Structured Tote / Crossbody
            // Curved Leather Handle
            val handlePath = Path().apply {
                moveTo(w * 0.36f, h * 0.34f)
                cubicTo(w * 0.36f, h * 0.14f, w * 0.64f, h * 0.14f, w * 0.64f, h * 0.34f)
            }
            drawPath(
                handlePath,
                color = seamLineColor,
                style = Stroke(width = 4.5f)
            )

            // Main Body with elegant corner curves
            drawRoundRect(
                color = baseColor,
                topLeft = Offset(w * 0.22f, h * 0.34f),
                size = Size(w * 0.56f, h * 0.46f),
                cornerRadius = CornerRadius(22f, 22f)
            )

            // Flap / Pocket accent
            drawLine(
                color = seamLineColor,
                start = Offset(w * 0.26f, h * 0.44f),
                end = Offset(w * 0.74f, h * 0.44f),
                strokeWidth = 2f
            )
            // Magnetic stud
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = 3.5f,
                center = Offset(w * 0.50f, h * 0.52f)
            )
        }
    }
}
