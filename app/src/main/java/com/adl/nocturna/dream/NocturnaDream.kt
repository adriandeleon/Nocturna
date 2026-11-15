package com.adl.nocturna.dream

import android.service.dreams.DreamService
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.adl.nocturna.clock.AndroidClockPlatform
import com.adl.nocturna.clock.ClockScreen
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.setBrightness
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.ui.NocturnaTheme

/**
 * The Android screen saver ("Daydream"). Pick it under Settings > Display > Screen saver and set it
 * to start while charging. It shows the same clock as the app, without the controls. With tap to peek on,
 * a tap on the dim clock brightens it for a moment, and a tap that doesn't peek ends the screen saver.
 */
class NocturnaDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Tap to peek needs the taps; without it, any touch ends the screen saver, as Android does it.
        val tapToPeek = SettingsStore.current.tapToPeek
        isInteractive = tapToPeek
        isFullscreen = true
        isScreenBright = true // we manage brightness ourselves

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@NocturnaDream)
            setViewTreeSavedStateRegistryOwner(this@NocturnaDream)
            setContent {
                NocturnaTheme {
                    CompositionLocalProvider(LocalClockPlatform provides AndroidClockPlatform(this@NocturnaDream)) {
                        ClockScreen(
                            interactive = false,
                            onBrightness = { window?.setBrightness(it) },
                            onDismiss = if (tapToPeek) ({ finish() }) else null,
                        )
                    }
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
