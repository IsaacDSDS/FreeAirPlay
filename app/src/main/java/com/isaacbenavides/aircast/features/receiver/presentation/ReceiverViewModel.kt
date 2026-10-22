package com.isaacbenavides.aircast.features.receiver.presentation

import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isaacbenavides.aircast.core.di.ServiceLocator
import com.isaacbenavides.aircast.core.media.AndroidRenderSurface
import com.isaacbenavides.aircast.features.receiver.domain.model.ReceiverState
import com.isaacbenavides.aircast.features.receiver.domain.repository.AirPlayReceiverRepository
import kotlinx.coroutines.flow.StateFlow

class ReceiverViewModel(private val repository: AirPlayReceiverRepository) : ViewModel() {

    val state: StateFlow<ReceiverState> = repository.state

    fun onSurfaceAvailable(surface: Surface) = repository.attachRenderSurface(AndroidRenderSurface(surface))

    fun onSurfaceDestroyed() = repository.attachRenderSurface(null)

    companion object {
        val Factory = viewModelFactory {
            initializer { ReceiverViewModel(ServiceLocator.receiverRepository) }
        }
    }
}
