package com.reverb.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.reverb.app.service.ReverbForegroundService
import com.reverb.core.designsystem.theme.ReverbTheme
import com.reverb.feature.onboarding.OnboardingRoute

/**
 * The only launcher activity. Hosts the Compose onboarding and settings flow
 * (API key, permissions, per tone writing samples) and shows the previous
 * crash log, if one was saved by [ReverbApplication].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ReverbApplication).container

        setContent {
            ReverbTheme {
                val prefs = getSharedPreferences("reverb_crash_logs", Context.MODE_PRIVATE)
                var crashLog by remember { mutableStateOf(prefs.getString("last_crash", null)) }

                if (crashLog != null) {
                    AlertDialog(
                        onDismissRequest = { },
                        confirmButton = {
                            TextButton(onClick = {
                                prefs.edit().remove("last_crash").apply()
                                crashLog = null
                            }) { Text("Clear & Continue") }
                        },
                        title = { Text("App Crashed Previously") },
                        text = {
                            Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
                                Text(crashLog ?: "")
                            }
                        },
                    )
                }

                OnboardingRoute(
                    apiKeyStorage = container.apiKeyStorage,
                    settingsStorage = container.settingsStorage,
                    versionName = BuildConfig.VERSION_NAME,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlaySettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                    onStartBackgroundService = {
                        // Stop the old one if running, then start fresh for a clean slate
                        val stopIntent = Intent(this, ReverbForegroundService::class.java)
                        stopService(stopIntent)
                        val serviceIntent = Intent(this, ReverbForegroundService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    },
                )
            }
        }
    }
}
