package com.shawkang.stackerassist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.concurrent.atomic.AtomicBoolean

class CaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "stacker_assist_capture"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    private var overlayText: TextView? = null
    private var markerView: TextView? = null
    private var markerParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null

    private val detector = MarkerStackDetector()
    private val processing = AtomicBoolean(false)

    private var currentSettings: AppSettings = AppSettings()
    private var lastSettingsLoadMs: Long = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentSettings = AppSettings.load(this)
        createNotificationChannel()
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startCapture(resultCode, data)
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        stopCapture()

        workerThread = HandlerThread("StackerCaptureWorker").also { it.start() }
        workerHandler = Handler(workerThread!!.looper)

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(projectionCallback, workerHandler)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            if (!processing.compareAndSet(false, true)) return@setOnImageAvailableListener

            val image = reader.acquireLatestImage()
            if (image == null) {
                processing.set(false)
                return@setOnImageAvailableListener
            }

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * image.width

                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                val now = System.currentTimeMillis()
                val settings = getFreshSettings(now)
                val signal = detector.analyze(cropped, now, settings)

                cropped.recycle()
                bitmap.recycle()

                if (signal.shouldTap && settings.autoTapEnabled) {
                    AutoTapBridge.requestTap(signal.tapX, signal.tapY, settings.tapDurationMs)
                }

                updateOverlay(signal, settings)
            } catch (_: Throwable) {
                updateOverlay(
                    StackerSignal(
                        status = "Frame error",
                        shouldTap = false,
                        tapX = 0f,
                        tapY = 0f
                    ),
                    currentSettings
                )
            } finally {
                image.close()
                processing.set(false)
            }
        }, workerHandler)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "StackerAssistDisplay",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            workerHandler
        )
    }

    private fun getFreshSettings(nowMs: Long): AppSettings {
        if (nowMs - lastSettingsLoadMs > 500L) {
            currentSettings = AppSettings.load(this)
            lastSettingsLoadMs = nowMs
        }
        return currentSettings
    }

    private fun createOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayText = TextView(this).apply {
            text = "Stacker Assist"
            textSize = 16f
            setTextColor(0xffffffff.toInt())
            setBackgroundColor(0xaa111111.toInt())
            setPadding(24, 16, 24, 16)
        }

        markerView = TextView(this).apply {
            text = "◆"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(0xffffd400.toInt())
            setBackgroundColor(0x00000000)
            visibility = View.GONE
        }

        val type = overlayType()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val textParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        markerParams = WindowManager.LayoutParams(
            70,
            70,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager?.addView(overlayText, textParams)
            windowManager?.addView(markerView, markerParams)
        } catch (_: Throwable) {
            overlayText = null
            markerView = null
            markerParams = null
        }
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun updateOverlay(signal: StackerSignal, settings: AppSettings) {
        overlayText?.post {
            val textView = overlayText ?: return@post

            if (signal.shouldTap) {
                val autoTapStatus = when {
                    !settings.autoTapEnabled -> "CUE ONLY"
                    AutoTapBridge.isReady() -> "AUTO TAP"
                    else -> "TAP SERVICE OFF"
                }
                textView.text = "$autoTapStatus  ${signal.status}"
                textView.textSize = 22f
                textView.setBackgroundColor(0xcc00aa00.toInt())
            } else {
                textView.text = signal.status
                textView.textSize = 16f
                textView.setBackgroundColor(0xaa111111.toInt())
            }

            updateMarkerOverlay(signal, settings)
        }
    }

    private fun updateMarkerOverlay(signal: StackerSignal, settings: AppSettings) {
        val marker = markerView ?: return
        val params = markerParams ?: return

        if (!settings.showVisualMarker || signal.markerX < 0f || signal.markerY < 0f) {
            marker.visibility = View.GONE
            return
        }

        marker.visibility = View.VISIBLE
        params.x = signal.markerX.toInt() - 35
        params.y = signal.markerY.toInt() + settings.visualMarkerYOffsetPx - 35

        try {
            windowManager?.updateViewLayout(marker, params)
        } catch (_: Throwable) {
            marker.visibility = View.GONE
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Stacker Assist running")
            .setContentText("Marker-contact detection active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stacker Assist Capture",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun stopCapture() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            workerThread?.quitSafely()
        } catch (_: Throwable) {
        }

        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        workerThread = null
        workerHandler = null
    }

    override fun onDestroy() {
        stopCapture()

        try {
            overlayText?.let { windowManager?.removeView(it) }
            markerView?.let { windowManager?.removeView(it) }
        } catch (_: Throwable) {
        }

        overlayText = null
        markerView = null
        markerParams = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
