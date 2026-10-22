package com.isaacbenavides.aircast.features.receiver.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Keeps the TV discoverable after a reboot or an app update, like a built-in receiver. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                ReceiverService.start(context)
                // Briefly visible (transparent) so the service may become a foreground service.
                runCatching {
                    context.startActivity(
                        Intent(context, BootActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                }.onFailure { Log.w("AirCastBoot", "could not open boot activity", it) }
            }
        }
    }
}
