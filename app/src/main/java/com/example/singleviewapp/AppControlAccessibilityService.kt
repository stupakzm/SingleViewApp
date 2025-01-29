package com.example.singleviewapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppControlAccessibilityService : AccessibilityService() {


    companion object {
        var instance: AppControlAccessibilityService? = null
        var currentForegroundApp: String? = null
        private val _accessAttemptsFlow = MutableStateFlow(0)
        val accessAttemptsFlow: StateFlow<Int> = _accessAttemptsFlow

        private var accessAttempts: MutableList<Long> = mutableListOf()

        private var lastIncrementTime: Long = 0 // Stores the time of the last increment
        private const val COOLDOWN_PERIOD_MS = 5000

        private fun resetCounter() {
            val currentTime = System.currentTimeMillis()
            accessAttempts = accessAttempts.filter {
                TimeUnit.MILLISECONDS.toHours(currentTime - it) < 24
            }.toMutableList()
        }

        private fun incrementCounter() {
            val currentTime = System.currentTimeMillis()
            // Check if the cooldown period has passed
            if (currentTime - lastIncrementTime < COOLDOWN_PERIOD_MS) {
                Log.d("AppControlService", "Cooldown active. Counter not incremented.")
                return
            }
            resetCounter()
            accessAttempts.add(currentTime)
            _accessAttemptsFlow.value = accessAttempts.size

            lastIncrementTime = currentTime
        }

        fun getCurrentCounter(): Int {
            resetCounter()
            return accessAttempts.size
        }

        fun updateAccessAttemptsFlow() {
            resetCounter()
            _accessAttemptsFlow.value = accessAttempts.size
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                // Ignore system launcher or system UI
                if (packageName.contains("launcher", true) || packageName.contains("systemui", true)) {
                    Log.d("AppControlService", "System launcher or UI detected, ignoring")
                    //currentForegroundApp = null
                    return
                }

                // Safely retrieve the app name from the package name
                val appName = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                } ?: return

                currentForegroundApp = appName

                // Debugging logs to verify service functionality
                Log.d("AppControlService", "App opened: $appName")
                Log.d("AppControlService", "Is in selected apps: ${InstalledAppsFetcher.getSelectedApps().contains(appName)}")

                // Check if the app is in the selected list and whether the timer is 0:0
                if (InstalledAppsFetcher.getSelectedApps().contains(appName)) {
                    var globalTimer = InstalledAppsFetcher.getGlobalTimer()
                    if (InstalledAppsFetcher.isGlobalTimerZero()) {
                        Log.d("AppControlService", "Blocking app: $appName (Timer: 0:0)")
                        incrementCounter()
                        Log.d("AppControlService", "Global attempt counter: ${getCurrentCounter()}")
                        performGlobalAction(GLOBAL_ACTION_HOME) // Send user to home screen
                        currentForegroundApp = null;
                    } else {
                        Log.d("AppControlService", "Allowing app: $appName (Timer active: ${globalTimer.first}:${globalTimer.second})")
                    }
                }
            }

            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Handle when no valid app is detected (e.g., all apps minimized)
                if (event.packageName == null) {
                    currentForegroundApp = null
                    Log.d("AppControlService", "No valid app detected, resetting currentForegroundApp")
                }
            }

            else -> {
                Log.d("AppControlService", "Unhandled event type: ${event.eventType}")
            }
        }
    }


    override fun onInterrupt() {
        Log.d("AppControlService", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this // Store a reference to the service instance
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    public fun sendToHomeScreen(){
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
