package com.isaacbenavides.aircast.core.di

import android.app.Application
import com.isaacbenavides.aircast.core.network.NetworkAddressProvider
import com.isaacbenavides.aircast.features.receiver.data.AirPlayReceiverRepositoryImpl
import com.isaacbenavides.aircast.features.receiver.data.identity.ReceiverIdentityStore
import com.isaacbenavides.aircast.features.receiver.domain.repository.AirPlayReceiverRepository

/** Composition root: the only place that knows concrete implementations. */
object ServiceLocator {

    private lateinit var application: Application

    fun init(application: Application) {
        this.application = application
    }

    val networkAddressProvider: NetworkAddressProvider by lazy { NetworkAddressProvider() }

    val receiverRepository: AirPlayReceiverRepository by lazy {
        AirPlayReceiverRepositoryImpl(application, ReceiverIdentityStore(application), networkAddressProvider)
    }
}
