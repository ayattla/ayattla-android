package com.ayattla.app.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

/** Returns a function that asks for coarse location once and delivers a fix (or null). */
@Composable
fun rememberLocationRequest(onResult: (Location?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentLocation(ctx, onResult) else onResult(null)
    }
    return {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            currentLocation(ctx, onResult)
        else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

@SuppressLint("MissingPermission")
private fun currentLocation(ctx: Context, onResult: (Location?) -> Unit) {
    val lm = ctx.getSystemService(LocationManager::class.java)
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    if (Build.VERSION.SDK_INT >= 30 && providers.isNotEmpty()) {
        runCatching {
            lm.getCurrentLocation(providers.first(), null, ContextCompat.getMainExecutor(ctx)) { loc -> onResult(loc ?: last) }
        }.onFailure { onResult(last) }
    } else onResult(last)
}
