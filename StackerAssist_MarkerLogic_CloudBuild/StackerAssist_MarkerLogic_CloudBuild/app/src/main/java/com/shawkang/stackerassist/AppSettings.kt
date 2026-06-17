package com.shawkang.stackerassist

import android.content.Context

data class AppSettings(
    val autoTapEnabled: Boolean = true,
    val showVisualMarker: Boolean = true,

    val saturationMin: Int = 70,
    val valueMin: Int = 95,

    val markerXOffsetPx: Int = 0,
    val markerYOffsetPx: Int = 0,
    val markerSampleRadiusPx: Int = 3,
    val markerColorChangeThreshold: Int = 85,

    val tapCooldownMs: Long = 260L,
    val tapDurationMs: Long = 45L,
    val levelAdvanceDelayMs: Long = 120L,

    val tapXRatio: Float = 0.50f,
    val tapYRatio: Float = 0.78f,

    val scanTopRatio: Float = 0.08f,
    val scanBottomRatio: Float = 0.90f,
    val minBandWidthRatio: Float = 0.03f,
    val minRowPixelsRatio: Float = 0.035f,
    val sampleStepPx: Int = 4,

    val minVerticalGapPx: Int = 18,
    val movingMatchYDistancePx: Int = 32,

    val visualMarkerYOffsetPx: Int = -42
) {
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_AUTO_TAP_ENABLED, autoTapEnabled)
            .putBoolean(KEY_SHOW_VISUAL_MARKER, showVisualMarker)
            .putInt(KEY_SATURATION_MIN, saturationMin)
            .putInt(KEY_VALUE_MIN, valueMin)
            .putInt(KEY_MARKER_X_OFFSET_PX, markerXOffsetPx)
            .putInt(KEY_MARKER_Y_OFFSET_PX, markerYOffsetPx)
            .putInt(KEY_MARKER_SAMPLE_RADIUS_PX, markerSampleRadiusPx)
            .putInt(KEY_MARKER_COLOR_CHANGE_THRESHOLD, markerColorChangeThreshold)
            .putLong(KEY_TAP_COOLDOWN_MS, tapCooldownMs)
            .putLong(KEY_TAP_DURATION_MS, tapDurationMs)
            .putLong(KEY_LEVEL_ADVANCE_DELAY_MS, levelAdvanceDelayMs)
            .putFloat(KEY_TAP_X_RATIO, tapXRatio)
            .putFloat(KEY_TAP_Y_RATIO, tapYRatio)
            .putFloat(KEY_SCAN_TOP_RATIO, scanTopRatio)
            .putFloat(KEY_SCAN_BOTTOM_RATIO, scanBottomRatio)
            .putFloat(KEY_MIN_BAND_WIDTH_RATIO, minBandWidthRatio)
            .putFloat(KEY_MIN_ROW_PIXELS_RATIO, minRowPixelsRatio)
            .putInt(KEY_SAMPLE_STEP_PX, sampleStepPx)
            .putInt(KEY_MIN_VERTICAL_GAP_PX, minVerticalGapPx)
            .putInt(KEY_MOVING_MATCH_Y_DISTANCE_PX, movingMatchYDistancePx)
            .putInt(KEY_VISUAL_MARKER_Y_OFFSET_PX, visualMarkerYOffsetPx)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "stacker_marker_settings"

        private const val KEY_AUTO_TAP_ENABLED = "autoTapEnabled"
        private const val KEY_SHOW_VISUAL_MARKER = "showVisualMarker"
        private const val KEY_SATURATION_MIN = "saturationMin"
        private const val KEY_VALUE_MIN = "valueMin"
        private const val KEY_MARKER_X_OFFSET_PX = "markerXOffsetPx"
        private const val KEY_MARKER_Y_OFFSET_PX = "markerYOffsetPx"
        private const val KEY_MARKER_SAMPLE_RADIUS_PX = "markerSampleRadiusPx"
        private const val KEY_MARKER_COLOR_CHANGE_THRESHOLD = "markerColorChangeThreshold"
        private const val KEY_TAP_COOLDOWN_MS = "tapCooldownMs"
        private const val KEY_TAP_DURATION_MS = "tapDurationMs"
        private const val KEY_LEVEL_ADVANCE_DELAY_MS = "levelAdvanceDelayMs"
        private const val KEY_TAP_X_RATIO = "tapXRatio"
        private const val KEY_TAP_Y_RATIO = "tapYRatio"
        private const val KEY_SCAN_TOP_RATIO = "scanTopRatio"
        private const val KEY_SCAN_BOTTOM_RATIO = "scanBottomRatio"
        private const val KEY_MIN_BAND_WIDTH_RATIO = "minBandWidthRatio"
        private const val KEY_MIN_ROW_PIXELS_RATIO = "minRowPixelsRatio"
        private const val KEY_SAMPLE_STEP_PX = "sampleStepPx"
        private const val KEY_MIN_VERTICAL_GAP_PX = "minVerticalGapPx"
        private const val KEY_MOVING_MATCH_Y_DISTANCE_PX = "movingMatchYDistancePx"
        private const val KEY_VISUAL_MARKER_Y_OFFSET_PX = "visualMarkerYOffsetPx"

        fun load(context: Context): AppSettings {
            val defaults = AppSettings()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            return AppSettings(
                autoTapEnabled = prefs.getBoolean(KEY_AUTO_TAP_ENABLED, defaults.autoTapEnabled),
                showVisualMarker = prefs.getBoolean(KEY_SHOW_VISUAL_MARKER, defaults.showVisualMarker),
                saturationMin = prefs.getInt(KEY_SATURATION_MIN, defaults.saturationMin),
                valueMin = prefs.getInt(KEY_VALUE_MIN, defaults.valueMin),
                markerXOffsetPx = prefs.getInt(KEY_MARKER_X_OFFSET_PX, defaults.markerXOffsetPx),
                markerYOffsetPx = prefs.getInt(KEY_MARKER_Y_OFFSET_PX, defaults.markerYOffsetPx),
                markerSampleRadiusPx = prefs.getInt(KEY_MARKER_SAMPLE_RADIUS_PX, defaults.markerSampleRadiusPx),
                markerColorChangeThreshold = prefs.getInt(KEY_MARKER_COLOR_CHANGE_THRESHOLD, defaults.markerColorChangeThreshold),
                tapCooldownMs = prefs.getLong(KEY_TAP_COOLDOWN_MS, defaults.tapCooldownMs),
                tapDurationMs = prefs.getLong(KEY_TAP_DURATION_MS, defaults.tapDurationMs),
                levelAdvanceDelayMs = prefs.getLong(KEY_LEVEL_ADVANCE_DELAY_MS, defaults.levelAdvanceDelayMs),
                tapXRatio = prefs.getFloat(KEY_TAP_X_RATIO, defaults.tapXRatio),
                tapYRatio = prefs.getFloat(KEY_TAP_Y_RATIO, defaults.tapYRatio),
                scanTopRatio = prefs.getFloat(KEY_SCAN_TOP_RATIO, defaults.scanTopRatio),
                scanBottomRatio = prefs.getFloat(KEY_SCAN_BOTTOM_RATIO, defaults.scanBottomRatio),
                minBandWidthRatio = prefs.getFloat(KEY_MIN_BAND_WIDTH_RATIO, defaults.minBandWidthRatio),
                minRowPixelsRatio = prefs.getFloat(KEY_MIN_ROW_PIXELS_RATIO, defaults.minRowPixelsRatio),
                sampleStepPx = prefs.getInt(KEY_SAMPLE_STEP_PX, defaults.sampleStepPx),
                minVerticalGapPx = prefs.getInt(KEY_MIN_VERTICAL_GAP_PX, defaults.minVerticalGapPx),
                movingMatchYDistancePx = prefs.getInt(KEY_MOVING_MATCH_Y_DISTANCE_PX, defaults.movingMatchYDistancePx),
                visualMarkerYOffsetPx = prefs.getInt(KEY_VISUAL_MARKER_Y_OFFSET_PX, defaults.visualMarkerYOffsetPx)
            ).sanitized()
        }
    }

    fun sanitized(): AppSettings {
        return copy(
            saturationMin = saturationMin.coerceIn(0, 255),
            valueMin = valueMin.coerceIn(0, 255),
            markerSampleRadiusPx = markerSampleRadiusPx.coerceIn(0, 20),
            markerColorChangeThreshold = markerColorChangeThreshold.coerceIn(1, 765),
            tapCooldownMs = tapCooldownMs.coerceIn(80L, 5000L),
            tapDurationMs = tapDurationMs.coerceIn(1L, 1000L),
            levelAdvanceDelayMs = levelAdvanceDelayMs.coerceIn(0L, 5000L),
            tapXRatio = tapXRatio.coerceIn(0.0f, 1.0f),
            tapYRatio = tapYRatio.coerceIn(0.0f, 1.0f),
            scanTopRatio = scanTopRatio.coerceIn(0.0f, 0.95f),
            scanBottomRatio = scanBottomRatio.coerceIn(0.05f, 1.0f),
            minBandWidthRatio = minBandWidthRatio.coerceIn(0.001f, 1.0f),
            minRowPixelsRatio = minRowPixelsRatio.coerceIn(0.001f, 1.0f),
            sampleStepPx = sampleStepPx.coerceIn(1, 16),
            minVerticalGapPx = minVerticalGapPx.coerceIn(0, 300),
            movingMatchYDistancePx = movingMatchYDistancePx.coerceIn(1, 300)
        )
    }
}
