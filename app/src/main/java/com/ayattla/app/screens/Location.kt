package com.ayattla.app.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
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

/**
 * A fix from every enabled source at once (fused, GPS, network), first answer wins, at most 25 s.
 * Asking only the first source failed on phones whose network source has no fix (GPS was never
 * asked), and Android 8–10 only used the last known fix, which is often empty.
 */
@SuppressLint("MissingPermission")
private fun currentLocation(ctx: Context, onResult: (Location?) -> Unit) {
    val lm = ctx.getSystemService(LocationManager::class.java)
    val sources = buildList {
        if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.GPS_PROVIDER); add(LocationManager.NETWORK_PROVIDER)
    }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    val last = (sources + LocationManager.PASSIVE_PROVIDER).distinct()
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    val main = Handler(Looper.getMainLooper())
    var done = false
    fun finish(l: Location?) { if (!done) { done = true; onResult(l) } }
    // A fix from the last few minutes is good enough for prayer times, qibla and mosques.
    if (last != null && System.currentTimeMillis() - last.time < 5 * 60_000) { finish(last); return }
    if (sources.isEmpty()) { finish(last); return }
    val signal = CancellationSignal()
    val listeners = mutableListOf<LocationListener>()
    var pending = sources.size
    fun answer(l: Location?) = main.post {
        if (l != null) { signal.cancel(); listeners.forEach { runCatching { lm.removeUpdates(it) } }; finish(l) }
        else if (--pending == 0) finish(last)
    }
    sources.forEach { p ->
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching { lm.getCurrentLocation(p, signal, ContextCompat.getMainExecutor(ctx)) { answer(it) } }.onFailure { answer(null) }
        } else {
            val l = object : LocationListener {
                override fun onLocationChanged(location: Location) { answer(location) }
                @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) { answer(null) }
            }
            listeners += l
            @Suppress("DEPRECATION")
            runCatching { lm.requestSingleUpdate(p, l, Looper.getMainLooper()) }.onFailure { answer(null) }
        }
    }
    main.postDelayed({ signal.cancel(); listeners.forEach { runCatching { lm.removeUpdates(it) } }; finish(last) }, 25_000)
}

/** Whether any location source is switched on (to tell "off" apart from "no fix"). */
fun locationEnabled(ctx: Context): Boolean {
    val lm = ctx.getSystemService(LocationManager::class.java)
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).any { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
}
