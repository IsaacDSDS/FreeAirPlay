package com.isaacbenavides.aircast.features.receiver.domain.model

/** What the receiver advertises to clients. */
data class ReceiverCapabilities(
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFps: Int,
    /** Apple clients only send H.265 when the receiver asks for more than 1080 lines. */
    val h265: Boolean,
    /** AirPlay video: the AirPlay button inside video apps and Safari (uxplay -hls). */
    val airPlayVideo: Boolean,
) {
    companion object {
        /** Up to 4K at 60 fps in H.265 (H.264 up to 1080p for clients without HEVC), no time limit. */
        val DEFAULT = ReceiverCapabilities(
            maxWidth = 3840,
            maxHeight = 2160,
            maxFps = 60,
            h265 = true,
            airPlayVideo = true,
        )
    }
}
