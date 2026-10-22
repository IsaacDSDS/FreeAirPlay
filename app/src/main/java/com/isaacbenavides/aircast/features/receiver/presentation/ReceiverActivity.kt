package com.isaacbenavides.aircast.features.receiver.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.isaacbenavides.aircast.R
import com.isaacbenavides.aircast.databinding.ActivityReceiverBinding
import com.isaacbenavides.aircast.features.receiver.domain.model.NowPlaying
import com.isaacbenavides.aircast.features.receiver.domain.model.ReceiverError
import com.isaacbenavides.aircast.features.receiver.domain.model.ReceiverPhase
import com.isaacbenavides.aircast.features.receiver.domain.model.ReceiverState
import com.isaacbenavides.aircast.features.receiver.domain.model.StreamPlayback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReceiverActivity : ComponentActivity() {

    private val viewModel: ReceiverViewModel by viewModels { ReceiverViewModel.Factory }
    private lateinit var binding: ActivityReceiverBinding

    /** Opened by the service because mirroring started: close again when it ends. */
    private var autoLaunched = false
    private var previousPhase: ReceiverPhase? = null
    private var previousClientId: String? = null
    private var shownArtwork: ByteArray? = null
    private var bannerJob: Job? = null
    private var progressJob: Job? = null
    private var playbackBarJob: Job? = null
    private var shownPlayback: StreamPlayback? = null

    /** Back closes an AirPlay video, like Menu on an Apple TV; otherwise the default applies. */
    private val stopStreamOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = viewModel.stopStream()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        autoLaunched = intent.getBooleanExtra(EXTRA_AUTO_LAUNCHED, false)
        onBackPressedDispatcher.addCallback(this, stopStreamOnBack)

        binding.videoSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = viewModel.onSurfaceAvailable(holder.surface)
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
            override fun surfaceDestroyed(holder: SurfaceHolder) = viewModel.onSurfaceDestroyed()
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Started while visible so TCL's boot manager lets the service go foreground.
        ReceiverService.start(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Only close on mirroring end if the service brought us up from the background,
        // not when the user already had the app open (onNewIntent precedes onRestart/onStart).
        val wasInBackground = !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (intent.getBooleanExtra(EXTRA_AUTO_LAUNCHED, false) && wasInBackground) autoLaunched = true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.state.value.phase != ReceiverPhase.STREAMING) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            -> if (event.repeatCount == 0) viewModel.togglePause()
            KeyEvent.KEYCODE_MEDIA_PLAY -> viewModel.setPaused(false)
            KeyEvent.KEYCODE_MEDIA_PAUSE -> viewModel.setPaused(true)
            KeyEvent.KEYCODE_DPAD_LEFT -> viewModel.seekBy(-SEEK_STEP_MS)
            KeyEvent.KEYCODE_DPAD_RIGHT -> viewModel.seekBy(SEEK_STEP_MS)
            KeyEvent.KEYCODE_MEDIA_REWIND -> viewModel.seekBy(-SEEK_LONG_STEP_MS)
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> viewModel.seekBy(SEEK_LONG_STEP_MS)
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                viewModel.stopStream()
                return true
            }
            // Only bring up the controls.
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_INFO -> Unit
            else -> return super.onKeyDown(keyCode, event)
        }
        showPlaybackBar()
        return true
    }

    private fun render(state: ReceiverState) {
        val videoPhase = state.phase.showsVideo()
        val showingVideo = videoPhase && state.videoSize != null
        val playingAudio = state.phase == ReceiverPhase.PLAYING_AUDIO
        val streaming = state.phase == ReceiverPhase.STREAMING

        binding.videoContainer.aspectRatio = state.videoSize?.aspectRatio ?: 0f
        binding.idlePanel.isVisible = !showingVideo && !playingAudio
        binding.audioPanel.isVisible = playingAudio && !showingVideo

        renderIdle(state)
        if (playingAudio) renderNowPlaying(state)
        renderPlayback(state)
        if (playingAudio || streaming) startProgressTicker() else stopProgressTicker()
        renderBanner(state)
        stopStreamOnBack.isEnabled = streaming

        val keepAwake = videoPhase || playingAudio
        if (keepAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val videoEnded = previousPhase?.showsVideo() == true && !videoPhase
        previousPhase = state.phase
        if (videoEnded && autoLaunched) {
            autoLaunched = false
            finish()
        }
    }

    private fun renderIdle(state: ReceiverState) {
        val name = state.receiverName.ifBlank { getString(R.string.app_name) }
        val client = state.client?.name ?: getString(R.string.client_unknown)
        binding.receiverName.text = name
        binding.stepChoose.text = getString(R.string.step_choose, name)
        binding.networkInfo.text = when {
            state.ipAddress == null -> getString(R.string.network_none)
            state.port > 0 -> getString(R.string.network_info, state.ipAddress, state.port)
            else -> state.ipAddress
        }
        val (text, dot) = when (state.phase) {
            ReceiverPhase.STARTING -> getString(R.string.status_starting) to R.color.warning
            ReceiverPhase.READY -> getString(R.string.status_ready) to R.color.success
            ReceiverPhase.CONNECTED -> getString(R.string.status_connected, client) to R.color.accent
            ReceiverPhase.MIRRORING -> getString(R.string.status_mirroring, client) to R.color.accent
            ReceiverPhase.STREAMING -> getString(R.string.status_streaming, client) to R.color.accent
            ReceiverPhase.PLAYING_AUDIO -> getString(R.string.status_audio, client) to R.color.accent
            ReceiverPhase.ERROR -> errorText(state.error) to R.color.error
        }
        binding.statusPill.text = text
        binding.statusPill.compoundDrawableTintList = colorList(dot)
    }

    private fun renderNowPlaying(state: ReceiverState) {
        val nowPlaying = state.nowPlaying ?: NowPlaying()
        binding.audioSource.text = getString(R.string.audio_source, state.client?.name ?: getString(R.string.client_unknown))
        binding.trackTitle.text = nowPlaying.title ?: getString(R.string.track_unknown)
        binding.trackArtist.text = nowPlaying.artist.orEmpty()
        binding.trackAlbum.text = nowPlaying.album.orEmpty()
        if (nowPlaying.artwork !== shownArtwork) {
            shownArtwork = nowPlaying.artwork
            val bitmap = nowPlaying.artwork?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap != null) {
                binding.artwork.setImageBitmap(bitmap)
                binding.artwork.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            } else {
                binding.artwork.setImageResource(R.drawable.ic_aircast)
                binding.artwork.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            }
            binding.artwork.clipToOutline = true
        }
    }

    private fun renderPlayback(state: ReceiverState) {
        val playback = state.playback.takeIf { state.phase == ReceiverPhase.STREAMING }
        if (playback == null) {
            playbackBarJob?.cancel()
            binding.playbackBar.isVisible = false
            shownPlayback = null
            return
        }
        binding.playbackIcon.setImageResource(if (playback.paused) R.drawable.ic_play else R.drawable.ic_pause)
        binding.playbackTitle.text = if (playback.buffering) {
            getString(R.string.playback_loading)
        } else {
            getString(R.string.status_streaming, state.client?.name ?: getString(R.string.client_unknown))
        }
        // Every report is a start, pause, seek or buffering change: worth showing the controls.
        if (playback != shownPlayback) {
            shownPlayback = playback
            showPlaybackBar()
        }
        updatePlaybackProgress(playback)
    }

    /** Shows the controls; they hide on their own only while the video is actually playing. */
    private fun showPlaybackBar() {
        binding.playbackBar.isVisible = true
        playbackBarJob?.cancel()
        val playback = viewModel.state.value.playback
        if (playback == null || playback.paused || playback.buffering) return
        playbackBarJob = lifecycleScope.launch {
            delay(PLAYBACK_BAR_TIMEOUT_MS)
            binding.playbackBar.isVisible = false
        }
    }

    private fun startProgressTicker() {
        if (progressJob?.isActive == true) return
        progressJob = lifecycleScope.launch {
            while (isActive) {
                val state = viewModel.state.value
                updateProgress(state.nowPlaying)
                state.playback?.let(::updatePlaybackProgress)
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateProgress(nowPlaying: NowPlaying?) {
        val duration = nowPlaying?.durationMs?.takeIf { it > 0 }
        val reported = nowPlaying?.positionMs
        binding.trackProgress.isVisible = duration != null
        binding.trackElapsed.isVisible = duration != null
        binding.trackDuration.isVisible = duration != null
        if (duration == null || reported == null) return
        val elapsed = (reported + SystemClock.elapsedRealtime() - nowPlaying.positionCapturedAtMs).coerceIn(0, duration)
        binding.trackProgress.progress = (elapsed * 1000 / duration).toInt()
        binding.trackElapsed.text = formatTime(elapsed)
        binding.trackDuration.text = formatTime(duration)
    }

    private fun updatePlaybackProgress(playback: StreamPlayback) {
        val duration = playback.durationMs.takeIf { it > 0 }
        val running = !playback.paused && !playback.buffering
        val advanced = if (running) SystemClock.elapsedRealtime() - playback.positionCapturedAtMs else 0
        val elapsed = (playback.positionMs + advanced).let { if (duration != null) it.coerceIn(0, duration) else it }
        binding.playbackProgress.isVisible = duration != null
        binding.playbackDuration.isVisible = duration != null
        binding.playbackElapsed.text = formatTime(elapsed)
        if (duration == null) return
        binding.playbackProgress.progress = (elapsed * 1000 / duration).toInt()
        binding.playbackDuration.text = formatTime(duration)
    }

    private fun renderBanner(state: ReceiverState) {
        val client = state.client
        if (client != null && client.deviceId != previousClientId) {
            binding.connectionBanner.text = getString(R.string.banner_connecting, client.name)
            binding.connectionBanner.visibility = View.VISIBLE
            bannerJob?.cancel()
            bannerJob = lifecycleScope.launch {
                delay(BANNER_DURATION_MS)
                binding.connectionBanner.visibility = View.GONE
            }
        }
        previousClientId = client?.deviceId
    }

    private fun errorText(error: ReceiverError?): String = getString(
        when (error) {
            ReceiverError.NETWORK_DISCOVERY -> R.string.status_error_discovery
            ReceiverError.SERVER -> R.string.status_error_server
            ReceiverError.UNKNOWN, null -> R.string.status_error_unknown
        },
    )

    private fun ReceiverPhase.showsVideo() = this == ReceiverPhase.MIRRORING || this == ReceiverPhase.STREAMING

    private fun colorList(@ColorRes color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds / 60 % 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
    }

    companion object {
        const val EXTRA_AUTO_LAUNCHED = "auto_launched"
        private const val BANNER_DURATION_MS = 3_000L
        private const val PROGRESS_TICK_MS = 500L
        private const val PLAYBACK_BAR_TIMEOUT_MS = 4_000L
        private const val SEEK_STEP_MS = 10_000L
        private const val SEEK_LONG_STEP_MS = 30_000L
    }
}
