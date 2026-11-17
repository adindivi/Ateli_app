package com.example.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import kotlin.math.abs

/**
 * BitmapUtils
 *
 * 단일 책임 원칙(SRP) 준수를 위한 가상 피팅 캔버스 비트맵 컷아웃 및 스케일링 전용 유틸리티.
 */
object BitmapUtils {

    /**
     * 의류 사진의 4개 코너 픽셀을 샘플링하여 스튜디오/바닥 배경색을 추정하고,
     * 매칭되는 배경을 투명하게 처리하며 가장자리를 부드럽게 페더링(Feathering) 처리합니다.
     */
    fun removeBackgroundFromGarmentBitmap(src: Bitmap, maxDimension: Int = 420): Bitmap {
        val scaled = scaleBitmapDown(src, maxDimension = maxDimension)
        val width = scaled.width
        val height = scaled.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        // 4개 모서리 픽셀을 샘플링하여 배경 RGB 추정
        val cornerIndices = listOf(
            0,
            width - 1,
            (height - 1) * width,
            (height - 1) * width + (width - 1)
        )
        var bgR = 0
        var bgG = 0
        var bgB = 0
        for (idx in cornerIndices) {
            val c = pixels[idx]
            bgR += AndroidColor.red(c)
            bgG += AndroidColor.green(c)
            bgB += AndroidColor.blue(c)
        }
        bgR /= cornerIndices.size
        bgG /= cornerIndices.size
        bgB /= cornerIndices.size

        val threshold = 48
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = AndroidColor.red(p)
            val g = AndroidColor.green(p)
            val b = AndroidColor.blue(p)
            val dist = (abs(r - bgR) + abs(g - bgG) + abs(b - bgB)) / 3
            if (dist < threshold) {
                pixels[i] = AndroidColor.argb(0, r, g, b)
            } else if (dist < threshold + 18) {
                val alpha = ((dist - threshold) * 255 / 18).coerceIn(0, 255)
                pixels[i] = AndroidColor.argb(alpha, r, g, b)
            }
        }
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * 가로/세로 비율을 유지하면서 指定된 maxDimension 이내로 안전하게 다운스케일링합니다.
     */
    fun scaleBitmapDown(src: Bitmap, maxDimension: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDimension && h <= maxDimension) return src
        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) {
            maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
        }
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
