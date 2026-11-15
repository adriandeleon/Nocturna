package com.adl.nocturna.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** The phone's rough location for the weather, when the user allowed it. */
object DeviceLocation {
    /**
     * A recent cached fix (under 6 hours old) or, failing that, a fresh one within 20 seconds; null
     * without the location permission or when no provider answers.
     */
    @SuppressLint("MissingPermission")
    suspend fun get(context: Context): Pair<Double, Double>? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val enabled = lm.getProviders(true)
        enabled.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 6 * 3600_000L }
            .maxByOrNull { it.time }
            ?.let { return it.latitude to it.longitude }

        // Nothing cached (common when no other app used location lately): ask for a fresh fix.
        val provider = listOf("fused", "network", LocationManager.GPS_PROVIDER).firstOrNull { it in enabled } ?: return null
        val fix = withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                runCatching {
                    LocationManagerCompat.getCurrentLocation(lm, provider, cancel, ContextCompat.getMainExecutor(context)) {
                        if (cont.isActive) cont.resume(it)
                    }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return fix?.let { it.latitude to it.longitude }
    }
}
