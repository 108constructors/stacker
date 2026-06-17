package com.shawkang.stackerassist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val captureRequestCode = 1001
    private val editors = mutableMapOf<String, EditText>()

    private lateinit var autoTapCheckBox: CheckBox
    private lateinit var visualMarkerCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 50)
        }

        buildUi()
    }

    private fun buildUi() {
        val settings = AppSettings.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }

        val title = TextView(this).apply {
            text = "Stacker Assist Marker Logic"
            textSize = 23f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        root.addView(title)

        val info = TextView(this).apply {
            text = "Marker logic: detect stack right edge → watch marker pixel → tap on colour change → switch upward."
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        root.addView(info)

        val permissionRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        permissionRow.addView(Button(this).apply {
            text = "Allow overlay"
            setOnClickListener { openOverlaySettings() }
        })

        permissionRow.addView(Button(this).apply {
            text = "Enable auto tap service"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(
                    this@MainActivity,
                    "Enable Stacker Assist in Accessibility settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        permissionRow.addView(Button(this).apply {
            text = "Start screen analysis"
            setOnClickListener { startScreenCaptureFlow() }
        })

        permissionRow.addView(Button(this).apply {
            text = "Stop"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                Toast.makeText(this@MainActivity, "Stopped", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(permissionRow)

        val settingsTitle = TextView(this).apply {
            text = "Settings"
            textSize = 20f
            setPadding(0, 30, 0, 12)
        }
        root.addView(settingsTitle)

        autoTapCheckBox = CheckBox(this).apply {
            text = "Auto tap enabled"
            isChecked = settings.autoTapEnabled
        }
        root.addView(autoTapCheckBox)

        visualMarkerCheckBox = CheckBox(this).apply {
            text = "Show visual marker"
            isChecked = settings.showVisualMarker
        }
        root.addView(visualMarkerCheckBox)

        addSetting(root, "saturationMin", "Block saturation min", settings.saturationMin.toString(), signed = false, decimal = false)
        addSetting(root, "valueMin", "Block brightness/value min", settings.valueMin.toString(), signed = false, decimal = false)

        addSetting(root, "markerXOffsetPx", "Marker X offset px", settings.markerXOffsetPx.toString(), signed = true, decimal = false)
        addSetting(root, "markerYOffsetPx", "Marker Y offset px", settings.markerYOffsetPx.toString(), signed = true, decimal = false)
        addSetting(root, "markerSampleRadiusPx", "Marker sample radius px", settings.markerSampleRadiusPx.toString(), signed = false, decimal = false)
        addSetting(root, "markerColorChangeThreshold", "Marker colour-change threshold", settings.markerColorChangeThreshold.toString(), signed = false, decimal = false)

        addSetting(root, "tapCooldownMs", "Tap cooldown ms", settings.tapCooldownMs.toString(), signed = false, decimal = false)
        addSetting(root, "tapDurationMs", "Tap duration ms", settings.tapDurationMs.toString(), signed = false, decimal = false)
        addSetting(root, "levelAdvanceDelayMs", "Level advance delay ms", settings.levelAdvanceDelayMs.toString(), signed = false, decimal = false)

        addSetting(root, "tapXRatio", "Tap X ratio", settings.tapXRatio.toString(), signed = false, decimal = true)
        addSetting(root, "tapYRatio", "Tap Y ratio", settings.tapYRatio.toString(), signed = false, decimal = true)

        addSetting(root, "scanTopRatio", "Scan top ratio", settings.scanTopRatio.toString(), signed = false, decimal = true)
        addSetting(root, "scanBottomRatio", "Scan bottom ratio", settings.scanBottomRatio.toString(), signed = false, decimal = true)
        addSetting(root, "minBandWidthRatio", "Minimum band width ratio", settings.minBandWidthRatio.toString(), signed = false, decimal = true)
        addSetting(root, "minRowPixelsRatio", "Minimum row pixel ratio", settings.minRowPixelsRatio.toString(), signed = false, decimal = true)

        addSetting(root, "sampleStepPx", "Pixel sample step px", settings.sampleStepPx.toString(), signed = false, decimal = false)
        addSetting(root, "minVerticalGapPx", "Minimum vertical gap px", settings.minVerticalGapPx.toString(), signed = false, decimal = false)
        addSetting(root, "movingMatchYDistancePx", "Moving match Y distance px", settings.movingMatchYDistancePx.toString(), signed = false, decimal = false)
        addSetting(root, "visualMarkerYOffsetPx", "Visual marker Y offset px", settings.visualMarkerYOffsetPx.toString(), signed = true, decimal = false)

        root.addView(Button(this).apply {
            text = "Save settings"
            setOnClickListener { saveSettings() }
        })

        root.addView(Button(this).apply {
            text = "Reset settings"
            setOnClickListener {
                AppSettings().save(this@MainActivity)
                editors.clear()
                buildUi()
                Toast.makeText(this@MainActivity, "Settings reset", Toast.LENGTH_SHORT).show()
            }
        })

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun addSetting(
        root: LinearLayout,
        key: String,
        label: String,
        value: String,
        signed: Boolean,
        decimal: Boolean
    ) {
        val labelView = TextView(this).apply {
            text = label
            textSize = 14f
            setPadding(0, 16, 0, 4)
        }

        var inputType = InputType.TYPE_CLASS_NUMBER
        if (signed) inputType = inputType or InputType.TYPE_NUMBER_FLAG_SIGNED
        if (decimal) inputType = inputType or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val editText = EditText(this).apply {
            setText(value)
            this.inputType = inputType
            setSingleLine(true)
        }

        editors[key] = editText
        root.addView(labelView)
        root.addView(editText)
    }

    private fun saveSettings() {
        val previous = AppSettings.load(this)

        val settings = AppSettings(
            autoTapEnabled = autoTapCheckBox.isChecked,
            showVisualMarker = visualMarkerCheckBox.isChecked,

            saturationMin = getInt("saturationMin", previous.saturationMin),
            valueMin = getInt("valueMin", previous.valueMin),

            markerXOffsetPx = getInt("markerXOffsetPx", previous.markerXOffsetPx),
            markerYOffsetPx = getInt("markerYOffsetPx", previous.markerYOffsetPx),
            markerSampleRadiusPx = getInt("markerSampleRadiusPx", previous.markerSampleRadiusPx),
            markerColorChangeThreshold = getInt("markerColorChangeThreshold", previous.markerColorChangeThreshold),

            tapCooldownMs = getLong("tapCooldownMs", previous.tapCooldownMs),
            tapDurationMs = getLong("tapDurationMs", previous.tapDurationMs),
            levelAdvanceDelayMs = getLong("levelAdvanceDelayMs", previous.levelAdvanceDelayMs),

            tapXRatio = getFloat("tapXRatio", previous.tapXRatio),
            tapYRatio = getFloat("tapYRatio", previous.tapYRatio),

            scanTopRatio = getFloat("scanTopRatio", previous.scanTopRatio),
            scanBottomRatio = getFloat("scanBottomRatio", previous.scanBottomRatio),
            minBandWidthRatio = getFloat("minBandWidthRatio", previous.minBandWidthRatio),
            minRowPixelsRatio = getFloat("minRowPixelsRatio", previous.minRowPixelsRatio),

            sampleStepPx = getInt("sampleStepPx", previous.sampleStepPx),
            minVerticalGapPx = getInt("minVerticalGapPx", previous.minVerticalGapPx),
            movingMatchYDistancePx = getInt("movingMatchYDistancePx", previous.movingMatchYDistancePx),
            visualMarkerYOffsetPx = getInt("visualMarkerYOffsetPx", previous.visualMarkerYOffsetPx)
        ).sanitized()

        settings.save(this)
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun getInt(key: String, fallback: Int): Int {
        return editors[key]?.text?.toString()?.trim()?.toIntOrNull() ?: fallback
    }

    private fun getLong(key: String, fallback: Long): Long {
        return editors[key]?.text?.toString()?.trim()?.toLongOrNull() ?: fallback
    }

    private fun getFloat(key: String, fallback: Float): Float {
        return editors[key]?.text?.toString()?.trim()?.toFloatOrNull() ?: fallback
    }

    private fun openOverlaySettings() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay already allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startScreenCaptureFlow() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings()
            Toast.makeText(this, "Allow overlay first, then come back.", Toast.LENGTH_LONG).show()
            return
        }

        if (!AutoTapBridge.isReady()) {
            Toast.makeText(
                this,
                "Auto tap service is not enabled. Detection still runs, but tapping will not work.",
                Toast.LENGTH_LONG
            ).show()
        }

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), captureRequestCode)
    }

    @Deprecated("startActivityForResult is used here to keep the MVP dependency-free.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != captureRequestCode || resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Screen capture not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, CaptureService::class.java).apply {
            putExtra(CaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(CaptureService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Open the stacker game now.", Toast.LENGTH_LONG).show()
    }
}
