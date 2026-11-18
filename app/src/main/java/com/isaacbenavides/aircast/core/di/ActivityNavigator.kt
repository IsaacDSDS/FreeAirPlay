package com.isaacbenavides.aircast.core.di

import android.content.Context
import android.content.Intent
import com.isaacbenavides.aircast.core.navigation.AppNavigator
import com.isaacbenavides.aircast.features.about.presentation.LicensesActivity

internal class ActivityNavigator : AppNavigator {

    override fun openLicenses(context: Context) =
        context.startActivity(Intent(context, LicensesActivity::class.java))
}
