package com.adl.nocturna.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/** Edge-to-edge with light system-bar icons; all our screens are dark whatever the system theme. */
fun ComponentActivity.enableDarkEdgeToEdge() {
    val transparent = android.graphics.Color.TRANSPARENT
    enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
}
