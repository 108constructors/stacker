package com.shawkang.stackerassist

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class StackerSignal(
    val status: String,
    val shouldTap: Boolean,
    val tapX: Float,
    val tapY: Float,
    val markerX: Float = -1f,
    val markerY: Float = -1f,
    val activeLevel: Int = 0
)

private data class Band(
    val centerY: Float,
    val minX: Int,
    val maxX: Int,
    val centerX: Float,
    val width: Int,
    val pixels: Int
)

class MarkerStackDetector {
    private var previousBands: List<Band> = emptyList()

    private var baselineColor: Int? = null
    private var lastMarkerX: Float = -9999f
    private var lastMarkerY: Float = -9999f
    private var markerWasBlock: Boolean = false

    private var lastTapMs: Long = 0L
    private var activeLevel: Int = 0

    fun analyze(bitmap: Bitmap, nowMs: Long, settings: AppSettings): StackerSignal {
        val s = settings.sanitized()

        val tapX = bitmap.width * s.tapXRatio
        val tapY = bitmap.height * s.tapYRatio

        if (s.scanBottomRatio <= s.scanTopRatio) {
            return StackerSignal(
                status = "Bad scan range: bottom must be greater than top",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                activeLevel = activeLevel
            )
        }

        val bands = findColoredBands(bitmap, s)
        if (bands.size < 2) {
            previousBands = bands
            baselineColor = null
            markerWasBlock = false
            return StackerSignal(
                status = "Searching bands: ${bands.size}",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                activeLevel = activeLevel
            )
        }

        val movingBand = chooseMovingBand(bands, s)
        val targetBand = chooseTargetBand(movingBand, bands, s)

        if (movingBand == null || targetBand == null) {
            previousBands = bands
            baselineColor = null
            markerWasBlock = false
            return StackerSignal(
                status = "Tracking stack...",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                activeLevel = activeLevel
            )
        }

        val markerX = (targetBand.maxX + s.markerXOffsetPx).coerceIn(0, bitmap.width - 1).toFloat()
        val markerY = (movingBand.centerY + s.markerYOffsetPx).toInt()
            .coerceIn(0, bitmap.height - 1)
            .toFloat()

        val markerMoved = abs(markerX - lastMarkerX) > max(6, s.markerSampleRadiusPx * 2) ||
            abs(markerY - lastMarkerY) > max(6, s.markerSampleRadiusPx * 2)

        if (markerMoved) {
            baselineColor = null
            markerWasBlock = false
            lastMarkerX = markerX
            lastMarkerY = markerY
        }

        val markerColor = averageColor(
            bitmap = bitmap,
            centerX = markerX.toInt(),
            centerY = markerY.toInt(),
            radius = s.markerSampleRadiusPx
        )

        val markerIsBlock = isBlockColor(markerColor, s)

        if (nowMs - lastTapMs < s.levelAdvanceDelayMs) {
            previousBands = bands
            return StackerSignal(
                status = "Placed level $activeLevel, switching marker up...",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                markerX = markerX,
                markerY = markerY,
                activeLevel = activeLevel
            )
        }

        if (baselineColor == null) {
            if (!markerIsBlock) {
                baselineColor = markerColor
                markerWasBlock = false
                previousBands = bands
                return StackerSignal(
                    status = "L$activeLevel marker armed at x=${markerX.toInt()}",
                    shouldTap = false,
                    tapX = tapX,
                    tapY = tapY,
                    markerX = markerX,
                    markerY = markerY,
                    activeLevel = activeLevel
                )
            }

            markerWasBlock = true
            previousBands = bands
            return StackerSignal(
                status = "L$activeLevel waiting marker clear",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                markerX = markerX,
                markerY = markerY,
                activeLevel = activeLevel
            )
        }

        if (!markerIsBlock) {
            baselineColor = markerColor
            markerWasBlock = false
            previousBands = bands
            return StackerSignal(
                status = "L$activeLevel marker clear",
                shouldTap = false,
                tapX = tapX,
                tapY = tapY,
                markerX = markerX,
                markerY = markerY,
                activeLevel = activeLevel
            )
        }

        val colorDiff = colorDistance(markerColor, baselineColor ?: markerColor)
        val isContactEdge = markerIsBlock && !markerWasBlock && colorDiff >= s.markerColorChangeThreshold
        val cooldownReady = nowMs - lastTapMs >= s.tapCooldownMs

        markerWasBlock = markerIsBlock
        previousBands = bands

        if (isContactEdge && cooldownReady) {
            lastTapMs = nowMs
            activeLevel += 1
            baselineColor = null
            markerWasBlock = false

            return StackerSignal(
                status = "AUTO TAP L$activeLevel diff=$colorDiff",
                shouldTap = true,
                tapX = tapX,
                tapY = tapY,
                markerX = markerX,
                markerY = markerY,
                activeLevel = activeLevel
            )
        }

        val reason = if (!cooldownReady) "cooldown" else "contact=$markerIsBlock diff=$colorDiff"
        return StackerSignal(
            status = "L$activeLevel $reason",
            shouldTap = false,
            tapX = tapX,
            tapY = tapY,
            markerX = markerX,
            markerY = markerY,
            activeLevel = activeLevel
        )
    }

    private fun findColoredBands(bitmap: Bitmap, settings: AppSettings): List<Band> {
        val width = bitmap.width
        val height = bitmap.height

        val top = (height * settings.scanTopRatio).toInt().coerceIn(0, height - 1)
        val bottom = (height * settings.scanBottomRatio).toInt().coerceIn(top + 1, height)

        val sampleStep = settings.sampleStepPx.coerceAtLeast(1)

        val rowCount = IntArray(height)
        val rowMinX = IntArray(height) { width }
        val rowMaxX = IntArray(height) { 0 }

        for (y in top until bottom step sampleStep) {
            var count = 0
            var minX = width
            var maxX = 0

            for (x in 0 until width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                if (isBlockColor(pixel, settings)) {
                    count += 1
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                }
            }

            rowCount[y] = count
            rowMinX[y] = minX
            rowMaxX[y] = maxX
        }

        val sampledColumns = max(1, width / sampleStep)
        val minRowPixels = (sampledColumns * settings.minRowPixelsRatio).toInt().coerceAtLeast(3)

        val bands = mutableListOf<Band>()
        var y = top

        while (y < bottom) {
            if (rowCount[y] < minRowPixels) {
                y += sampleStep
                continue
            }

            val startY = y
            var endY = y
            var pixels = 0
            var minX = width
            var maxX = 0

            while (y < bottom && rowCount[y] >= minRowPixels) {
                pixels += rowCount[y]
                minX = min(minX, rowMinX[y])
                maxX = max(maxX, rowMaxX[y])
                endY = y
                y += sampleStep
            }

            val bandHeight = endY - startY
            val bandWidth = maxX - minX

            if (bandHeight >= sampleStep && bandWidth > width * settings.minBandWidthRatio) {
                bands += Band(
                    centerY = (startY + endY) / 2f,
                    minX = minX,
                    maxX = maxX,
                    centerX = (minX + maxX) / 2f,
                    width = bandWidth,
                    pixels = pixels
                )
            }
        }

        return bands.sortedBy { it.centerY }
    }

    private fun chooseMovingBand(currentBands: List<Band>, settings: AppSettings): Band? {
        if (currentBands.isEmpty()) return null

        if (previousBands.isEmpty()) {
            return currentBands.minByOrNull { it.centerY }
        }

        var best: Band? = null
        var bestMotion = 0f

        for (band in currentBands) {
            val previous = previousBands.minByOrNull { abs(it.centerY - band.centerY) } ?: continue
            val yClose = abs(previous.centerY - band.centerY) <= settings.movingMatchYDistancePx
            if (!yClose) continue

            val motion = abs(previous.centerX - band.centerX)
            if (motion > bestMotion) {
                bestMotion = motion
                best = band
            }
        }

        return best ?: currentBands.minByOrNull { it.centerY }
    }

    private fun chooseTargetBand(movingBand: Band?, bands: List<Band>, settings: AppSettings): Band? {
        if (movingBand == null) return null

        val below = bands
            .filter { it.centerY > movingBand.centerY + settings.minVerticalGapPx }
            .minByOrNull { it.centerY - movingBand.centerY }

        if (below != null) return below

        return bands
            .filter { it != movingBand }
            .minByOrNull { abs(it.centerY - movingBand.centerY) }
    }

    private fun isBlockColor(pixel: Int, settings: AppSettings): Boolean {
        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff

        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val saturation = maxC - minC
        val value = maxC

        return saturation >= settings.saturationMin && value >= settings.valueMin
    }

    private fun averageColor(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int): Int {
        val r = radius.coerceAtLeast(0)
        var totalR = 0
        var totalG = 0
        var totalB = 0
        var count = 0

        val left = (centerX - r).coerceIn(0, bitmap.width - 1)
        val right = (centerX + r).coerceIn(0, bitmap.width - 1)
        val top = (centerY - r).coerceIn(0, bitmap.height - 1)
        val bottom = (centerY + r).coerceIn(0, bitmap.height - 1)

        for (y in top..bottom) {
            for (x in left..right) {
                val pixel = bitmap.getPixel(x, y)
                totalR += (pixel shr 16) and 0xff
                totalG += (pixel shr 8) and 0xff
                totalB += pixel and 0xff
                count += 1
            }
        }

        if (count == 0) return 0xff000000.toInt()

        val avgR = totalR / count
        val avgG = totalG / count
        val avgB = totalB / count

        return 0xff000000.toInt() or (avgR shl 16) or (avgG shl 8) or avgB
    }

    private fun colorDistance(a: Int, b: Int): Int {
        val ar = (a shr 16) and 0xff
        val ag = (a shr 8) and 0xff
        val ab = a and 0xff

        val br = (b shr 16) and 0xff
        val bg = (b shr 8) and 0xff
        val bb = b and 0xff

        return abs(ar - br) + abs(ag - bg) + abs(ab - bb)
    }
}
