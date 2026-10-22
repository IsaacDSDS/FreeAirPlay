package com.isaacbenavides.aircast.features.receiver.presentation

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Invisible activity opened at boot. TCL's firmware only allows a third-party foreground service
 * while the app has a visible activity, so the receiver service is started from here and the
 * activity closes itself a moment later.
 */
class BootActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        ReceiverService.start(this)
        handler.postDelayed(::finishQuietly, VISIBLE_MS)
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
        if (!isFinishing) finishQuietly()
    }

    private fun finishQuietly() {
        finish()
        overridePendingTransition(0, 0)
    }

    private companion object {
        const val VISIBLE_MS = 1_500L
    }
}
