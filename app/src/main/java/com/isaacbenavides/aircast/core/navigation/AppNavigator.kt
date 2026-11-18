package com.isaacbenavides.aircast.core.navigation

import android.content.Context

/** Opens screens that belong to other features, so features never import each other. */
interface AppNavigator {
    fun openLicenses(context: Context)
}
