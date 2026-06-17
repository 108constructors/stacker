package com.shawkang.stackerassist

import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong

object AutoTapBridge {
    private var serviceRef: WeakReference<AutoTapAccessibilityService>? = null
    private val lastRequestMs = AtomicLong(0)

    fun attach(service: AutoTapAccessibilityService) {
        serviceRef = WeakReference(service)
    }

    fun detach(service: AutoTapAccessibilityService) {
        if (serviceRef?.get() == service) {
            serviceRef = null
        }
    }

    fun isReady(): Boolean {
        return serviceRef?.get() != null
    }

    fun requestTap(x: Float, y: Float, durationMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastRequestMs.get()

        if (now - previous < 80L) {
            return false
        }

        if (!lastRequestMs.compareAndSet(previous, now)) {
            return false
        }

        val service = serviceRef?.get() ?: return false
        return service.tap(x, y, durationMs)
    }
}
