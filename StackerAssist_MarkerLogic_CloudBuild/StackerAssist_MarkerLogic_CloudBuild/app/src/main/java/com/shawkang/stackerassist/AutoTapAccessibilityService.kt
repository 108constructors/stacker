package com.shawkang.stackerassist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class AutoTapAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AutoTapBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This service does not read UI content.
        // It only dispatches a tap gesture when requested by the detector.
    }

    override fun onInterrupt() {
        // Nothing to interrupt.
    }

    override fun onDestroy() {
        AutoTapBridge.detach(this)
        super.onDestroy()
    }

    fun tap(x: Float, y: Float, durationMs: Long): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    durationMs.coerceIn(1L, 1000L)
                )
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }
}
