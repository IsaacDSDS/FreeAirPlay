package com.isaacbenavides.aircast.features.receiver.domain.repository

import com.isaacbenavides.aircast.features.receiver.domain.model.ReceiverState
import kotlinx.coroutines.flow.StateFlow

/** Opaque render target supplied by the platform layer (an Android Surface on Android). */
interface RenderSurface

interface AirPlayReceiverRepository {
    val state: StateFlow<ReceiverState>

    /** Starts advertising the receiver and accepting AirPlay clients. Blocking; call off the main thread. */
    fun start()

    /** Disconnects clients and stops advertising. Blocking; call off the main thread. */
    fun stop()

    /** Restarts the server, e.g. after the TV's IP address changed. Blocking. */
    fun restart()

    /** Where mirrored video is drawn; null while nothing is on screen (decoding continues off-screen). */
    fun attachRenderSurface(surface: RenderSurface?)
}
