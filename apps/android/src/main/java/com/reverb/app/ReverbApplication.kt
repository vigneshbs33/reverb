package com.reverb.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.reverb.app.di.AppContainer

/**
 * Application entry point.
 *
 * Installs a global uncaught exception handler that saves the last stack trace to
 * private shared preferences, so [MainActivity] can show it on the next launch.
 * The original handler is still called afterwards. Also owns the [AppContainer].
 */
class ReverbApplication : Application() {

    /** The app's dependencies, built on first use. See [AppContainer]. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("ReverbCrash", "Fatal crash caught: ${exception.message}", exception)

            try {
                val prefs = getSharedPreferences("reverb_crash_logs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_crash", android.util.Log.getStackTraceString(exception)).commit()
            } catch (e: Exception) {
                Log.e("ReverbCrash", "Failed to save crash log", e)
            }

            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
