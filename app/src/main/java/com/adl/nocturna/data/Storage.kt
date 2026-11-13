package com.adl.nocturna.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager

object Storage {
    /**
     * Preferences in device-protected storage. Unlike normal app storage it can be read right after a
     * reboot, before the user unlocks the phone, so alarms still ring after an overnight restart.
     * Files written by older versions to normal storage are moved over the first time we run unlocked.
     */
    fun prefs(context: Context, name: String): SharedPreferences {
        val app = context.applicationContext
        val deviceStorage = app.createDeviceProtectedStorageContext()
        if (app.getSystemService(UserManager::class.java).isUserUnlocked) {
            runCatching { deviceStorage.moveSharedPreferencesFrom(app, name) }
        }
        return deviceStorage.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    fun isUnlocked(context: Context): Boolean =
        context.getSystemService(UserManager::class.java).isUserUnlocked

    /** Per-device flags that must not be restored from a backup onto a new phone. */
    fun localPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("nocturna_local", Context.MODE_PRIVATE)
}
